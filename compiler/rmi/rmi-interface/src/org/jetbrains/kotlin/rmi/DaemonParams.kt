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
import kotlin.reflect.KMutableProperty1


public val COMPILER_JAR_NAME: String = "kotlin-compiler.jar"
public val COMPILER_SERVICE_RMI_NAME: String = "KotlinJvmCompilerService"
public val COMPILER_DAEMON_CLASS_FQN: String = "org.jetbrains.kotlin.rmi.service.CompileDaemon"
public val COMPILE_DAEMON_DEFAULT_PORT: Int = 17031
public val COMPILE_DAEMON_ENABLED_PROPERTY: String ="kotlin.daemon.enabled"
public val COMPILE_DAEMON_JVM_OPTIONS_PROPERTY: String ="kotlin.daemon.jvm.options"
public val COMPILE_DAEMON_OPTIONS_PROPERTY: String ="kotlin.daemon.options"
public val COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX: String ="--daemon-"
public val COMPILE_DAEMON_TIMEOUT_INFINITE_S: Int = 0
public val COMPILE_DAEMON_MEMORY_THRESHOLD_INFINITE: Long = 0L

val COMPILER_ID_DIGEST = "MD5"

//open class PropExtractor<C, V, P: KProperty1<C, V>>(val dest: C,
//                                                    val prop: P,
//                                                    val name: String,
//                                                    val convert: ((v: V) -> String?) = { it.toString() },
//                                                    val skipIf: ((v: V) -> Boolean) = { false },
//                                                    val mergeWithDelimiter: String? = null)
//{
//    constructor(dest: C, prop: P, convert: ((v: V) -> String?) = { it.toString() }, skipIf: ((v: V) -> Boolean) = { false }) : this(dest, prop, prop.name, convert, skipIf)
//    open fun extract(prefix: String = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX): List<String> =
//            when {
//                skipIf(prop.get(dest)) -> listOf<String>()
//                mergeWithDelimiter != null -> listOf(prefix + name + mergeWithDelimiter + convert(prop.get(dest))).filterNotNull()
//                else -> listOf(prefix + name, convert(prop.get(dest))).filterNotNull()
//            }
//}
//
//class BoolPropExtractor<C, P: KMutableProperty1<C, Boolean>>(dest: C, prop: P, name: String? = null)
//    : PropExtractor<C, Boolean, P>(dest, prop, name ?: prop.name, convert = { null }, skipIf = { !prop.get(dest) })
//
//class RestPropExtractor<C, P: KMutableProperty1<C, out MutableCollection<String>>>(dest: C, prop: P) : PropExtractor<C, MutableCollection<String>, P>(dest, prop, convert = { null }) {
//    override fun extract(prefix: String): List<String> = prop.get(dest).map { prefix + it }
//}
//
//
//open class PropParser<C, V, P: KMutableProperty1<C, V>>(val dest: C,
//                                                        val prop: P, alternativeNames: List<String>,
//                                                        val parse: (s: String) -> V,
//                                                        val allowMergedArg: Boolean = false) {
//    val names = listOf(prop.name) + alternativeNames
//    constructor(dest: C, prop: P, parse: (s: String) -> V, allowMergedArg: Boolean = false) : this(dest, prop, listOf(), parse, allowMergedArg)
//    fun apply(s: String) = prop.set(dest, parse(s))
//}
//
//class BoolPropParser<C, P: KMutableProperty1<C, Boolean>>(dest: C, prop: P): PropParser<C, Boolean, P>(dest, prop, { true })
//
//class RestPropParser<C, P: KMutableProperty1<C, MutableCollection<String>>>(dest: C, prop: P): PropParser<C, MutableCollection<String>, P>(dest, prop, { arrayListOf() }) {
//    fun add(s: String) { prop.get(dest).add(s) }
//}

// --------------------------------------------------------

open class PropMapper<C, V, P: KMutableProperty1<C, V>>(val dest: C,
                                                        val prop: P,
                                                        val names: List<String> = listOf(prop.name),
                                                        val fromString: (s: String) -> V,
                                                        val toString: ((v: V) -> String?) = { it.toString() },
                                                        val skipIf: ((v: V) -> Boolean) = { false },
                                                        val mergeDelimiter: String? = null)
{
    open fun toArgs(prefix: String = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX): List<String> =
            when {
                skipIf(prop.get(dest)) -> listOf<String>()
                mergeDelimiter != null -> listOf(prefix + names.first() + mergeDelimiter + toString(prop.get(dest))).filterNotNull()
                else -> listOf(prefix + names.first(), toString(prop.get(dest))).filterNotNull()
            }
    fun apply(s: String) = prop.set(dest, fromString(s))
}

class StringPropMapper<C, P: KMutableProperty1<C, String>>(dest: C,
                                                           prop: P,
                                                           names: List<String> = listOf(),
                                                           fromString: ((String) -> String) = { it },
                                                           toString: ((String) -> String?) = { it.toString() },
                                                           skipIf: ((String) -> Boolean) = { it.isEmpty() },
                                                           mergeDelimiter: String? = null)
: PropMapper<C, String, P>(dest = dest, prop = prop, names = if (names.any()) names else listOf(prop.name),
                            fromString = fromString,  toString = toString, skipIf = skipIf, mergeDelimiter = mergeDelimiter)

class BoolPropMapper<C, P: KMutableProperty1<C, Boolean>>(dest: C, prop: P, names: List<String> = listOf())
    : PropMapper<C, Boolean, P>(dest = dest, prop = prop, names = if (names.any()) names else listOf(prop.name), 
                                fromString = { true },  toString = { null }, skipIf = { !prop.get(dest) })

class RestPropMapper<C, P: KMutableProperty1<C, MutableCollection<String>>>(dest: C, prop: P) 
    : PropMapper<C, MutableCollection<String>, P>(dest = dest, prop = prop, toString = { null }, fromString = { arrayListOf() }) 
{
    override fun toArgs(prefix: String): List<String> = prop.get(dest).map { prefix + it }
    fun add(s: String) { prop.get(dest).add(s) }
}

// ------------------------------------------

fun Iterable<String>.filterExtractProps(propMappers: List<PropMapper<*,*,*>>, prefix: String, restParser: RestPropMapper<*,*>? = null) : Iterable<String>  {
    var currentPropMapper: PropMapper<*,*,*>? = null
    var matchingOption = ""
    val res = filter { param ->
        if (currentPropMapper == null) {
            val propMapper = propMappers.find {
                it !is RestPropMapper<*,*> &&
                it.names.any { name ->
                    if (param.startsWith(prefix + name)) {
                        matchingOption = prefix + name
                        true
                    }
                    else {
                        false
                    }
                }
            }
            when {
                propMapper != null -> {
                    val optionLength = matchingOption.length()
                    when {
                        propMapper is BoolPropMapper<*,*> -> {
                            if (param.length() > optionLength)
                                throw IllegalArgumentException("Invalid switch option '$param', expecting $matchingOption without arguments")
                            propMapper.apply("")
                        }
                        param.length() > optionLength ->
                            if (param[optionLength] != '=') {
                                if (propMapper.mergeDelimiter == null)
                                    throw IllegalArgumentException("Invalid option syntax '$param', expecting $matchingOption[= ]<arg>")
                                propMapper.apply(param.substring(optionLength))
                            }
                            else {
                                propMapper.apply(param.substring(optionLength + 1))
                            }
                        else ->
                            currentPropMapper = propMapper
                    }
                    false
                }
                restParser != null && param.startsWith(prefix) -> {
                    restParser.add(param.removePrefix(prefix))
                    false
                }
                else -> true
            }
        }
        else {
            currentPropMapper!!.apply(param)
            currentPropMapper = null
            false
        }
    }
    if (currentPropMapper != null) 
        throw IllegalArgumentException("Expecting argument for the option $matchingOption")
    return res
}

// TODO: find out how to create more generic variant using first constructor
//fun<C> C.propsToParams() {
//    val kc = C::class
//    kc.constructors.first().
//}



public interface OptionsGroup : Serializable {
    public val mappers: List<PropMapper<*,*,*>>
}

public fun Iterable<String>.filterExtractProps(vararg groups: OptionsGroup, prefix: String) : Iterable<String> =
        filterExtractProps(groups.flatMap { it.mappers }, prefix)


public data class DaemonJVMOptions(
        public var maxMemory: String = "",
        public var maxPermSize: String = "",
        public var reservedCodeCacheSize: String = "",
        public var otherJvmParams: MutableCollection<String> = arrayListOf()
) : OptionsGroup {

    override val mappers: List<PropMapper<*,*,*>>
        get() = listOf( StringPropMapper(this, ::maxMemory, listOf("Xmx"), mergeDelimiter = ""),
                        StringPropMapper(this, ::maxPermSize, listOf("XX:MaxPermSize"), mergeDelimiter = "="),
                        StringPropMapper(this, ::reservedCodeCacheSize, listOf("XX:ReservedCodeCacheSize"), mergeDelimiter = "="),
                        restMapper)

    val restMapper: RestPropMapper<*,*>
        get() = RestPropMapper(this, ::otherJvmParams)
}

public data class DaemonOptions(
        public var port: Int = COMPILE_DAEMON_DEFAULT_PORT,
        public var autoshutdownMemoryThreshold: Long = COMPILE_DAEMON_MEMORY_THRESHOLD_INFINITE,
        public var autoshutdownIdleSeconds: Int = COMPILE_DAEMON_TIMEOUT_INFINITE_S,
        public var startEcho: String = COMPILER_SERVICE_RMI_NAME
) : OptionsGroup {

    override val mappers: List<PropMapper<*, *, *>>
        get() = listOf( PropMapper(this, ::port, fromString = { it.toInt() }),
                        PropMapper(this, ::autoshutdownMemoryThreshold, fromString = { it.toLong() }, skipIf = { it == 0L }),
                        PropMapper(this, ::autoshutdownIdleSeconds, fromString = { it.toInt() }, skipIf = { it == 0 }),
                        PropMapper(this, ::startEcho, fromString = { it.trim('"') }))
}


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
) : OptionsGroup {

    override val mappers: List<PropMapper<*, *, *>>
        get() = listOf( PropMapper(this, ::compilerClasspath, toString = { it.joinToString(File.pathSeparator) }, fromString = { it.trim('"').split(File.pathSeparator)}),
                        StringPropMapper(this, ::compilerDigest),
                        StringPropMapper(this, ::compilerVersion))

    public fun updateDigest() {
        compilerDigest = compilerClasspath.getClasspathDigest()
    }

    companion object {
        public jvmStatic fun makeCompilerId(vararg paths: File): CompilerId = makeCompilerId(paths.asIterable())

        public jvmStatic fun makeCompilerId(paths: Iterable<File>): CompilerId =
                // TODO consider reading version here
                CompilerId(compilerClasspath = paths.map { it.absolutePath }, compilerDigest = paths.getFilesClasspathDigest())
    }
}


public fun isDaemonEnabled(): Boolean = System.getProperty(COMPILE_DAEMON_ENABLED_PROPERTY) != null


public fun configureDaemonLaunchingOptions(opts: DaemonJVMOptions, inheritMemoryLimits: Boolean): DaemonJVMOptions {
    // note: sequence matters, explicit override in COMPILE_DAEMON_JVM_OPTIONS_PROPERTY should be done after inputArguments processing
    if (inheritMemoryLimits)
        ManagementFactory.getRuntimeMXBean().inputArguments.filterExtractProps(opts.mappers, "-")

    System.getProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY)?.let {
        opts.otherJvmParams.addAll( it.trim('"', '\'').split(",").filterExtractProps(opts.mappers, "-", opts.restMapper))
    }
    return opts
}

public fun configureDaemonLaunchingOptions(inheritMemoryLimits: Boolean): DaemonJVMOptions =
    configureDaemonLaunchingOptions(DaemonJVMOptions(), inheritMemoryLimits = inheritMemoryLimits)

jvmOverloads public fun configureDaemonOptions(opts: DaemonOptions = DaemonOptions()): DaemonOptions {
    System.getProperty(COMPILE_DAEMON_OPTIONS_PROPERTY)?.let {
        val unrecognized = it.trim('"', '\'').split(",").filterExtractProps(opts.mappers, "")
        if (unrecognized.any())
            throw IllegalArgumentException(
                    "Unrecognized daemon options passed via property $COMPILE_DAEMON_OPTIONS_PROPERTY: " + unrecognized.joinToString(" ") +
                    "\nSupported options: " + opts.mappers.joinToString(", ", transform = { it.names.first() }))
    }
    return opts
}

