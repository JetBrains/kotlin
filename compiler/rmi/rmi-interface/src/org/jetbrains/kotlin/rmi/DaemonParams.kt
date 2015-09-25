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
public val COMPILE_DAEMON_FIND_PORT_ATTEMPTS: Int = 10
public val COMPILE_DAEMON_PORTS_RANGE_START: Int = 17001
public val COMPILE_DAEMON_PORTS_RANGE_END: Int = 18000
public val COMPILE_DAEMON_STARTUP_LOCK_TIMEOUT_MS: Long = 10000L
public val COMPILE_DAEMON_STARTUP_LOCK_TIMEOUT_CHECK_MS: Long = 100L
public val COMPILE_DAEMON_ENABLED_PROPERTY: String = "kotlin.daemon.enabled"
public val COMPILE_DAEMON_JVM_OPTIONS_PROPERTY: String = "kotlin.daemon.jvm.options"
public val COMPILE_DAEMON_OPTIONS_PROPERTY: String = "kotlin.daemon.options"
public val COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY: String = "kotlin.daemon.client.alive.path"
public val COMPILE_DAEMON_LOG_PATH_PROPERTY: String = "kotlin.daemon.log.path"
public val COMPILE_DAEMON_REPORT_PERF_PROPERTY: String = "kotlin.daemon.perf"
public val COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY: String = "kotlin.daemon.verbose"
public val COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX: String = "--daemon-"
public val COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY: String = "kotlin.daemon.startup.timeout"
public val COMPILE_DAEMON_DEFAULT_FILES_PREFIX: String = "kotlin-daemon"
public val COMPILE_DAEMON_DATA_DIRECTORY_NAME: String = "." + COMPILE_DAEMON_DEFAULT_FILES_PREFIX
public val COMPILE_DAEMON_TIMEOUT_INFINITE_S: Int = 0
public val COMPILE_DAEMON_DEFAULT_IDLE_TIMEOUT_S: Int = 7200 // 2 hours
public val COMPILE_DAEMON_MEMORY_THRESHOLD_INFINITE: Long = 0L

public val COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH: String get() =
    // TODO consider special case for windows - local appdata
    File(System.getProperty("user.home"), COMPILE_DAEMON_DATA_DIRECTORY_NAME).absolutePath

val COMPILER_ID_DIGEST = "MD5"


public fun makeRunFilenameString(timestamp: String, digest: String, port: String, escapeSequence: String = ""): String = "$COMPILE_DAEMON_DEFAULT_FILES_PREFIX$escapeSequence.$timestamp$escapeSequence.$digest$escapeSequence.$port$escapeSequence.run"


open class PropMapper<C, V, P : KMutableProperty1<C, V>>(val dest: C,
                                                         val prop: P,
                                                         val names: List<String> = listOf(prop.name),
                                                         val fromString: (String) -> V,
                                                         val toString: ((V) -> String?) = { it.toString() },
                                                         val skipIf: ((V) -> Boolean) = { false },
                                                         val mergeDelimiter: String? = null) {
    open fun toArgs(prefix: String = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX): List<String> =
            when {
                skipIf(prop.get(dest)) -> listOf<String>()
                mergeDelimiter != null -> listOf(listOf(prefix + names.first(), toString(prop.get(dest))).filterNotNull().joinToString(mergeDelimiter))
                else -> listOf(prefix + names.first(), toString(prop.get(dest))).filterNotNull()
            }

    open fun apply(s: String) = prop.set(dest, fromString(s))
}


class NullablePropMapper<C, V : Any?, P : KMutableProperty1<C, V>>(dest: C,
                                                                   prop: P,
                                                                   names: List<String> = listOf(),
                                                                   fromString: ((String) -> V),
                                                                   toString: ((V) -> String?) = { it.toString() },
                                                                   skipIf: ((V) -> Boolean) = { it == null },
                                                                   mergeDelimiter: String? = null)
: PropMapper<C, V, P>(dest = dest, prop = prop, names = if (names.any()) names else listOf(prop.name),
                      fromString = fromString, toString = toString, skipIf = skipIf, mergeDelimiter = mergeDelimiter)


class StringPropMapper<C, P : KMutableProperty1<C, String>>(dest: C,
                                                            prop: P,
                                                            names: List<String> = listOf(),
                                                            fromString: ((String) -> String) = { it },
                                                            toString: ((String) -> String?) = { it.toString() },
                                                            skipIf: ((String) -> Boolean) = { it.isEmpty() },
                                                            mergeDelimiter: String? = null)
: PropMapper<C, String, P>(dest = dest, prop = prop, names = if (names.any()) names else listOf(prop.name),
                           fromString = fromString, toString = toString, skipIf = skipIf, mergeDelimiter = mergeDelimiter)


class BoolPropMapper<C, P : KMutableProperty1<C, Boolean>>(dest: C, prop: P, names: List<String> = listOf())
: PropMapper<C, Boolean, P>(dest = dest, prop = prop, names = if (names.any()) names else listOf(prop.name),
                            fromString = { true }, toString = { null }, skipIf = { !prop.get(dest) })


class RestPropMapper<C, P : KMutableProperty1<C, MutableCollection<String>>>(dest: C, prop: P)
: PropMapper<C, MutableCollection<String>, P>(dest = dest, prop = prop, toString = { null }, fromString = { arrayListOf() }) {
    override fun toArgs(prefix: String): List<String> = prop.get(dest).map { prefix + it }
    override fun apply(s: String) = add(s)
    fun add(s: String) {
        prop.get(dest).add(s)
    }
}


// helper function combining find with map, useful for the cases then there is a calculation performed in find, which is nice to return along with
// found value; mappingPredicate should return the pair of boolean compare predicate result and transformation value, we want to get along with found value
inline fun <T, R : Any> Iterable<T>.findWithTransform(mappingPredicate: (T) -> Pair<Boolean, R?>): R? {
    for (element in this) {
        val (found, mapped) = mappingPredicate(element)
        if (found) return mapped
    }
    return null
}


// filter-like function, takes list of propmappers, bound to properties of concrete objects, iterates over receiver, extract matching values via appropriate
// mappers into bound properties; if restParser is given, adds all non-matching elements to it, otherwise return them as an iterable
// note bound properties mutation!
fun Iterable<String>.filterExtractProps(propMappers: List<PropMapper<*, *, *>>, prefix: String, restParser: RestPropMapper<*, *>? = null): Iterable<String> {

    val iter = iterator()
    val rest = arrayListOf<String>()

    while (iter.hasNext()) {
        val param = iter.next()
        val (propMapper, matchingOption) = propMappers.findWithTransform { mapper ->
            mapper.names
                    .firstOrNull { param.startsWith(prefix + it) }
                    .let { Pair(it != null, Pair(mapper, it)) }
        } ?: Pair(null, null)

        when {
            propMapper != null -> {
                val optionLength = prefix.length() + matchingOption!!.length()
                when {
                    propMapper is BoolPropMapper<*, *> -> {
                        if (param.length() > optionLength)
                            throw IllegalArgumentException("Invalid switch option '$param', expecting $prefix$matchingOption without arguments")
                        propMapper.apply("")
                    }
                    param.length() > optionLength ->
                        if (param[optionLength] != '=') {
                            if (propMapper.mergeDelimiter == null)
                                throw IllegalArgumentException("Invalid option syntax '$param', expecting $prefix$matchingOption[= ]<arg>")
                            propMapper.apply(param.substring(optionLength))
                        }
                        else {
                            propMapper.apply(param.substring(optionLength + 1))
                        }
                    else -> {
                        if (!iter.hasNext()) throw IllegalArgumentException("Expecting argument for the option $prefix$matchingOption")
                        propMapper.apply(iter.next())
                    }
                }
            }
            restParser != null && param.startsWith(prefix) ->
                restParser.add(param.removePrefix(prefix))
            else -> rest.add(param)
        }
    }
    return rest
}


public fun String.trimQuotes() = trim('"','\'')


public interface OptionsGroup : Serializable {
    public val mappers: List<PropMapper<*, *, *>>
}

public fun Iterable<String>.filterExtractProps(vararg groups: OptionsGroup, prefix: String): Iterable<String> =
        filterExtractProps(groups.flatMap { it.mappers }, prefix)


public data class DaemonJVMOptions(
        public var maxMemory: String = "",
        public var maxPermSize: String = "",
        public var reservedCodeCacheSize: String = "",
        public var jvmParams: MutableCollection<String> = arrayListOf()
) : OptionsGroup {

    override val mappers: List<PropMapper<*, *, *>>
        get() = listOf(StringPropMapper(this, ::maxMemory, listOf("Xmx"), mergeDelimiter = ""),
                       StringPropMapper(this, ::maxPermSize, listOf("XX:MaxPermSize"), mergeDelimiter = "="),
                       StringPropMapper(this, ::reservedCodeCacheSize, listOf("XX:ReservedCodeCacheSize"), mergeDelimiter = "="),
                       restMapper)

    val restMapper: RestPropMapper<*, *>
        get() = RestPropMapper(this, ::jvmParams)
}


public data class DaemonOptions(
        public var runFilesPath: String = COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH,
        public var autoshutdownMemoryThreshold: Long = COMPILE_DAEMON_MEMORY_THRESHOLD_INFINITE,
        public var autoshutdownIdleSeconds: Int = COMPILE_DAEMON_DEFAULT_IDLE_TIMEOUT_S,
        public var clientAliveFlagPath: String? = null
) : OptionsGroup {

    override val mappers: List<PropMapper<*, *, *>>
        get() = listOf(PropMapper(this, ::runFilesPath, fromString = { it.trimQuotes() }),
                       PropMapper(this, ::autoshutdownMemoryThreshold, fromString = { it.toLong() }, skipIf = { it == 0L }, mergeDelimiter = "="),
                       PropMapper(this, ::autoshutdownIdleSeconds, fromString = { it.toInt() }, skipIf = { it == 0 }, mergeDelimiter = "="),
                       NullablePropMapper(this, ::clientAliveFlagPath, fromString = { it }, toString = { "${it?.trimQuotes()}" }, mergeDelimiter = "="))
}


fun updateSingleFileDigest(file: File, md: MessageDigest) {
    DigestInputStream(file.inputStream(), md).use {
        val buf = ByteArray(1024)
        while (it.read(buf) != -1) {}
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
        (entry.extension.equals("class", ignoreCase = true) ||
         entry.extension.equals("jar", ignoreCase = true))
            -> updateSingleFileDigest(entry, md)
    // else skip
    }
}

@JvmName("getFilesClasspathDigest_Files")
fun Iterable<File>.getFilesClasspathDigest(): String {
    val md = MessageDigest.getInstance(COMPILER_ID_DIGEST)
    this.forEach { updateEntryDigest(it, md) }
    return md.digest().joinToString("", transform = { "%02x".format(it) })
}

@JvmName("getFilesClasspathDigest_Strings")
fun Iterable<String>.getFilesClasspathDigest(): String = map { File(it) }.getFilesClasspathDigest()

fun Iterable<String>.distinctStringsDigest(): String =
        MessageDigest.getInstance(COMPILER_ID_DIGEST)
                .digest(this.distinct().sorted().joinToString("").toByteArray())
                .joinToString("", transform = { "%02x".format(it) })


public data class CompilerId(
        public var compilerClasspath: List<String> = listOf(),
        public var compilerDigest: String = "",
        public var compilerVersion: String = ""
) : OptionsGroup {

    override val mappers: List<PropMapper<*, *, *>>
        get() = listOf(PropMapper(this, ::compilerClasspath, toString = { it.joinToString(File.pathSeparator) }, fromString = { it.trimQuotes().split(File.pathSeparator) }),
                       StringPropMapper(this, ::compilerDigest),
                       StringPropMapper(this, ::compilerVersion))

    public fun updateDigest() {
        compilerDigest = compilerClasspath.getFilesClasspathDigest()
    }

    companion object {
        @JvmStatic
        public fun makeCompilerId(vararg paths: File): CompilerId = makeCompilerId(paths.asIterable())

        @JvmStatic
        public fun makeCompilerId(paths: Iterable<File>): CompilerId =
                CompilerId(compilerClasspath = paths.map { it.absolutePath }, compilerDigest = paths.getFilesClasspathDigest())
    }
}


public fun isDaemonEnabled(): Boolean = System.getProperty(COMPILE_DAEMON_ENABLED_PROPERTY) != null


public fun configureDaemonJVMOptions(opts: DaemonJVMOptions, inheritMemoryLimits: Boolean, vararg additionalParams: String): DaemonJVMOptions {
    // note: sequence matters, explicit override in COMPILE_DAEMON_JVM_OPTIONS_PROPERTY should be done after inputArguments processing
    if (inheritMemoryLimits) {
        ManagementFactory.getRuntimeMXBean().inputArguments.filterExtractProps(opts.mappers, "-")
    }
    System.getProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY)?.let {
        opts.jvmParams.addAll(
                it.trimQuotes()
                  .split("(?<!\\\\),".toRegex())  // using independent non-capturing group with negative lookahead zero length assertion to split only on non-escaped commas
                  .map { it.replace("\\\\(.)".toRegex(), "$1") } // de-escaping characters escaped by backslash, straightforward, without exceptions
                  .filterExtractProps(opts.mappers, "-", opts.restMapper))
    }

    System.getProperty(COMPILE_DAEMON_REPORT_PERF_PROPERTY)?.let { opts.jvmParams.add("D" + COMPILE_DAEMON_REPORT_PERF_PROPERTY) }
    System.getProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY)?.let { opts.jvmParams.add("D" + COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY) }
    System.getProperty(COMPILE_DAEMON_LOG_PATH_PROPERTY)?.let { opts.jvmParams.add("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"$it\"" ) }
    opts.jvmParams.addAll(additionalParams)
    return opts
}


public fun configureDaemonJVMOptions(inheritMemoryLimits: Boolean, vararg additionalParams: String): DaemonJVMOptions =
        configureDaemonJVMOptions(DaemonJVMOptions(), inheritMemoryLimits = inheritMemoryLimits, additionalParams = *additionalParams)


public fun configureDaemonOptions(opts: DaemonOptions): DaemonOptions {
    System.getProperty(COMPILE_DAEMON_OPTIONS_PROPERTY)?.let {
        val unrecognized = it.trimQuotes().split(",").filterExtractProps(opts.mappers, "")
        if (unrecognized.any())
            throw IllegalArgumentException(
                    "Unrecognized daemon options passed via property $COMPILE_DAEMON_OPTIONS_PROPERTY: " + unrecognized.joinToString(" ") +
                    "\nSupported options: " + opts.mappers.joinToString(", ", transform = { it.names.first() }))
    }
    System.getProperty(COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY)?.let {
        val trimmed = it.trimQuotes()
        if (!trimmed.isBlank()) {
            opts.clientAliveFlagPath = trimmed
        }
    }
    return opts
}


public fun configureDaemonOptions(): DaemonOptions = configureDaemonOptions(DaemonOptions())
