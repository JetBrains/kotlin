/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.rmi

import java.io.File
import java.io.Serializable
import java.lang.management.ManagementFactory
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.platform.platformStatic
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1


public val COMPILER_JAR_NAME: String = "kotlin-compiler.jar"
public val COMPILER_SERVICE_RMI_NAME: String = "KotlinJvmCompilerService"
public val COMPILER_DAEMON_CLASS_FQN: String = "org.jetbrains.kotlin.rmi.service.CompileDaemon"
public val COMPILE_DAEMON_DEFAULT_PORT: Int = 17031
public val COMPILE_DAEMON_ENABLED_PROPERTY: String ="kotlin.daemon.enabled"
public val COMPILE_DAEMON_JVM_OPTIONS_PROPERTY: String ="kotlin.daemon.jvm.options"
public val COMPILE_DAEMON_OPTIONS_PROPERTY: String ="kotlin.daemon.options"
public val COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX: String ="--daemon-"


open class PropExtractor<C, V, P: KProperty1<C, V>>(val dest: C,
                                                    val prop: P,
                                                    val name: String,
                                                    val convert: ((v: V) -> String?) = { it.toString() },
                                                    val skipIf: ((v: V) -> Boolean) = { false },
                                                    val mergeWithDelimiter: String? = null)
{
    constructor(dest: C, prop: P, convert: ((v: V) -> String?) = { it.toString() }, skipIf: ((v: V) -> Boolean) = { false }) : this(dest, prop, prop.name, convert, skipIf)
    open fun extract(prefix: String = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX): List<String> =
            when {
                skipIf(prop.get(dest)) -> listOf<String>()
                mergeWithDelimiter != null -> listOf(prefix + name + mergeWithDelimiter + convert(prop.get(dest))).filterNotNull()
                else -> listOf(prefix + name, convert(prop.get(dest))).filterNotNull()
            }
}

class BoolPropExtractor<C, P: KMutableProperty1<C, Boolean>>(dest: C, prop: P, name: String? = null)
    : PropExtractor<C, Boolean, P>(dest, prop, name ?: prop.name, convert = { null }, skipIf = { !prop.get(dest) })

class RestPropExtractor<C, P: KMutableProperty1<C, out MutableCollection<String>>>(dest: C, prop: P) : PropExtractor<C, MutableCollection<String>, P>(dest, prop, convert = { null }) {
    override fun extract(prefix: String): List<String> = prop.get(dest).map { prefix + it }
}


open class PropParser<C, V, P: KMutableProperty1<C, V>>(val dest: C,
                                                        val prop: P, alternativeNames: List<String>,
                                                        val parse: (s: String) -> V,
                                                        val allowMergedArg: Boolean = false) {
    val names = listOf(prop.name) + alternativeNames
    constructor(dest: C, prop: P, parse: (s: String) -> V, allowMergedArg: Boolean = false) : this(dest, prop, listOf(), parse, allowMergedArg)
    fun apply(s: String) = prop.set(dest, parse(s))
}

class BoolPropParser<C, P: KMutableProperty1<C, Boolean>>(dest: C, prop: P): PropParser<C, Boolean, P>(dest, prop, { true })

class RestPropParser<C, P: KMutableProperty1<C, MutableCollection<String>>>(dest: C, prop: P): PropParser<C, MutableCollection<String>, P>(dest, prop, { arrayListOf() }) {
    fun add(s: String) { prop.get(dest).add(s) }
}


fun Iterable<String>.filterSetProps(parsers: List<PropParser<*,*,*>>, prefix: String, restParser: RestPropParser<*,*>? = null) : Iterable<String>  {
    var currentParser: PropParser<*,*,*>? = null
    var matchingOption = ""
    val res = filter { param ->
        if (currentParser == null) {
            val parser = parsers.find { it.names.any { name ->
                if (param.startsWith(prefix + name)) { matchingOption = prefix + name; true }
                else false } }
            if (parser != null) {
                val optionLength = matchingOption.length()
                when {
                    parser is BoolPropParser<*,*> ->
                        if (param.length() > optionLength) throw IllegalArgumentException("Invalid switch option '$param', expecting $matchingOption without arguments")
                        else parser.apply("")
                    param.length() > optionLength ->
                        if (param[optionLength] != '=') {
                            if (parser.allowMergedArg) parser.apply(param.substring(optionLength))
                            else throw IllegalArgumentException("Invalid option syntax '$param', expecting $matchingOption[= ]<arg>")
                        }
                        else parser.apply(param.substring(optionLength + 1))
                    else -> currentParser = parser
                }
                false
            }
            else if (restParser != null && param.startsWith(prefix)) {
                restParser.add(param.removePrefix(prefix))
                false
            }
            else true
        }
        else {
            currentParser!!.apply(param)
            currentParser = null
            false
        }
    }
    if (currentParser != null) throw IllegalArgumentException("Expecting argument for the option $matchingOption")
    return res
}

// TODO: find out how to create more generic variant using first constructor
//fun<C> C.propsToParams() {
//    val kc = C::class
//    kc.constructors.first().
//}



public interface CmdlineParams : Serializable {
    public val extractors: List<PropExtractor<*,*,*>>
    public val parsers: List<PropParser<*,*,*>>
}

public fun Iterable<String>.filterSetProps(vararg cs: CmdlineParams, prefix: String) : Iterable<String> =
    filterSetProps(cs.flatMap { it.parsers }, prefix)


public data class DaemonLaunchingOptions(
        public var maxMemory: String = "",
        public var maxPermSize: String = "",
        public var reservedCodeCacheSize: String = "",
        public var otherJvmParams: MutableCollection<String> = arrayListOf()
) : CmdlineParams {

    override val extractors: List<PropExtractor<*,*,*>>
        get() = listOf( PropExtractor(this, ::maxMemory, "Xmx", skipIf = { it.isEmpty() }, mergeWithDelimiter = ""),
                        PropExtractor(this, ::maxPermSize, "XX:MaxPermSize", skipIf = { it.isEmpty() }, mergeWithDelimiter = "="),
                        PropExtractor(this, ::reservedCodeCacheSize, "XX:ReservedCodeCacheSize", skipIf = { it.isEmpty() }, mergeWithDelimiter = "="),
                        RestPropExtractor(this, ::otherJvmParams))

    override val parsers: List<PropParser<*,*,*>>
        get() = listOf( PropParser(this, ::maxMemory, listOf("Xmx"), { it }, allowMergedArg = true),
                        PropParser(this, ::maxPermSize, listOf("XX:MaxPermSize"), { it }, allowMergedArg = true),
                        PropParser(this, ::reservedCodeCacheSize, listOf("XX:ReservedCodeCacheSize"), { it }, allowMergedArg = true))
                        // otherJvmParams is missing here deliberately, it is used explicitly as a restParser param to filterSetProps
}

public data class DaemonOptions(
        public var port: Int = COMPILE_DAEMON_DEFAULT_PORT,
        public var autoshutdownMemoryThreshold: Long = 0 /* 0 means unchecked */,
        public var autoshutdownIdleSeconds: Int = 0 /* 0 means unchecked */,
        public var startEcho: String = COMPILER_SERVICE_RMI_NAME
) : CmdlineParams {

    override val extractors: List<PropExtractor<*, *, *>>
        get() = listOf( PropExtractor(this, ::port),
                        PropExtractor(this, ::autoshutdownMemoryThreshold, skipIf = { it == 0L }),
                        PropExtractor(this, ::autoshutdownIdleSeconds, skipIf = { it == 0 }),
                        PropExtractor(this, ::startEcho))

    override val parsers: List<PropParser<*,*,*>>
            get() = listOf( PropParser(this, ::port, { it.toInt()}),
                            PropParser(this, ::autoshutdownMemoryThreshold, { it.toLong()}),
                            PropParser(this, ::autoshutdownIdleSeconds, { it.toInt()}),
                            PropParser(this, ::startEcho, { it.trim('"') }))
}


val COMPILER_ID_DIGEST = "MD5"


fun updateSingleFileDigest(file: File, md: MessageDigest) {
    DigestInputStream(file.inputStream(), md).use {
        val buf = ByteArray(1024)
        while (it.read(buf) != -1) { }
        it.close()
    }
}

fun updateForAllClasses(dir: File, md: MessageDigest) {
    dir.walk().forEach { updateEntryDigest(it, md) }
}

fun updateEntryDigest(entry: File, md: MessageDigest) {
    when {
        entry.isDirectory
            -> updateForAllClasses(entry, md)
        entry.isFile &&
                (entry.getName().endsWith(".class", ignoreCase = true) ||
                entry.getName().endsWith(".jar", ignoreCase = true))
            -> updateSingleFileDigest(entry, md)
        // else skip
    }
}

fun Iterable<File>.getFilesClasspathDigest(): String {
    val md = MessageDigest.getInstance(COMPILER_ID_DIGEST)
    this.forEach { updateEntryDigest(it, md) }
    return md.digest().joinToString("", transform = { "%02x".format(it) })
}

fun Iterable<String>.getClasspathDigest(): String = map { File(it) }.getFilesClasspathDigest()


public data class CompilerId(
        public var compilerClasspath: List<String> = listOf(),
        public var compilerDigest: String = "",
        public var compilerVersion: String = ""
        // TODO: checksum
) : CmdlineParams {

    override val extractors: List<PropExtractor<*, *, *>>
        get() = listOf( PropExtractor(this, ::compilerClasspath, convert = { it.joinToString(File.pathSeparator) }),
                        PropExtractor(this, ::compilerDigest),
                        PropExtractor(this, ::compilerVersion, skipIf = { it.isEmpty() }))

    override val parsers: List<PropParser<*,*,*>>
        get() =
            listOf( PropParser(this, ::compilerClasspath, { it.trim('"').split(File.pathSeparator)}),
                    PropParser(this, ::compilerDigest, { it.trim('"') }),
                    PropParser(this, ::compilerVersion, { it.trim('"') }))

    public fun updateDigest() {
        compilerDigest = compilerClasspath.getClasspathDigest()
    }

    companion object {
        public platformStatic fun makeCompilerId(vararg paths: File): CompilerId = makeCompilerId(paths.asIterable())

        public platformStatic fun makeCompilerId(paths: Iterable<File>): CompilerId =
                // TODO consider reading version here
                CompilerId(compilerClasspath = paths.map { it.absolutePath }, compilerDigest = paths.getFilesClasspathDigest())
    }
}


public fun isDaemonEnabled(): Boolean = System.getProperty(COMPILE_DAEMON_ENABLED_PROPERTY) != null


public fun configureDaemonLaunchingOptions(opts: DaemonLaunchingOptions, inheritMemoryLimits: Boolean): DaemonLaunchingOptions {
    // note: sequence matters, explicit override in COMPILE_DAEMON_JVM_OPTIONS_PROPERTY should be done after inputArguments processing
    if (inheritMemoryLimits)
        ManagementFactory.getRuntimeMXBean().inputArguments.filterSetProps(opts.parsers, "-")

    System.getProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY)?.let {
        opts.otherJvmParams.addAll( it.trim('"', '\'').split(",").filterSetProps(opts.parsers, "-", RestPropParser(opts, DaemonLaunchingOptions::otherJvmParams)))
    }
    return opts
}

public fun configureDaemonLaunchingOptions(inheritMemoryLimits: Boolean): DaemonLaunchingOptions =
    configureDaemonLaunchingOptions(DaemonLaunchingOptions(), inheritMemoryLimits = inheritMemoryLimits)

jvmOverloads public fun configureDaemonOptions(opts: DaemonOptions = DaemonOptions()): DaemonOptions {
    System.getProperty(COMPILE_DAEMON_OPTIONS_PROPERTY)?.let {
        val unrecognized = it.trim('"', '\'').split(",").filterSetProps(opts.parsers, "")
        if (unrecognized.any())
            throw IllegalArgumentException(
                    "Unrecognized daemon options passed via property $COMPILE_DAEMON_OPTIONS_PROPERTY: " + unrecognized.joinToString(" ") +
                    "\nSupported options: " + opts.extractors.joinToString(", ", transform = { it.name }))
    }
    return opts
}

