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

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import java.io.File
import java.io.Serializable
import java.lang.management.ManagementFactory
import java.security.MessageDigest
import java.util.*
import kotlin.reflect.KMutableProperty1


val COMPILER_JAR_NAME: String = "kotlin-compiler.jar"
val COMPILER_SERVICE_RMI_NAME: String = "KotlinJvmCompilerService"
val COMPILER_DAEMON_CLASS_FQN: String = "org.jetbrains.kotlin.daemon.KotlinCompileDaemon"
val COMPILE_DAEMON_FIND_PORT_ATTEMPTS: Int = 10
val COMPILE_DAEMON_PORTS_RANGE_START: Int = 17001
val COMPILE_DAEMON_PORTS_RANGE_END: Int = 18000
val COMPILE_DAEMON_ENABLED_PROPERTY: String = "kotlin.daemon.enabled"
val COMPILE_DAEMON_JVM_OPTIONS_PROPERTY: String = "kotlin.daemon.jvm.options"
val COMPILE_DAEMON_OPTIONS_PROPERTY: String = "kotlin.daemon.options"
val COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY: String = "kotlin.daemon.client.alive.path"
val COMPILE_DAEMON_LOG_PATH_PROPERTY: String = "kotlin.daemon.log.path"
val COMPILE_DAEMON_REPORT_PERF_PROPERTY: String = "kotlin.daemon.perf"
val COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY: String = "kotlin.daemon.verbose"
val COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX: String = "--daemon-"
val COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY: String = "kotlin.daemon.startup.timeout"
val COMPILE_DAEMON_DEFAULT_FILES_PREFIX: String = "kotlin-daemon"
val COMPILE_DAEMON_TIMEOUT_INFINITE_S: Int = 0
val COMPILE_DAEMON_DEFAULT_IDLE_TIMEOUT_S: Int = 7200 // 2 hours
val COMPILE_DAEMON_DEFAULT_UNUSED_TIMEOUT_S: Int = 60
val COMPILE_DAEMON_DEFAULT_SHUTDOWN_DELAY_MS: Long = 1000L // 1 sec
val COMPILE_DAEMON_MEMORY_THRESHOLD_INFINITE: Long = 0L
val COMPILE_DAEMON_FORCE_SHUTDOWN_DEFAULT_TIMEOUT_MS: Long = 10000L // 10 secs
val COMPILE_DAEMON_TIMEOUT_INFINITE_MS: Long = 0L
val COMPILE_DAEMON_IS_READY_MESSAGE = "Kotlin compile daemon is ready"

val COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH: String get() =
    FileSystem.getRuntimeStateFilesPath("kotlin", "daemon")

val CLASSPATH_ID_DIGEST = "MD5"


open class PropMapper<C, V, out P : KMutableProperty1<C, V>>(val dest: C,
                                                             val prop: P,
                                                             val names: List<String> = listOf(prop.name),
                                                             val fromString: (String) -> V,
                                                             val toString: ((V) -> String?) = { it.toString() },
                                                             val skipIf: ((V) -> Boolean) = { false },
                                                             val mergeDelimiter: String? = null) {
    open fun toArgs(prefix: String = COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX): List<String> =
            when {
                skipIf(prop.get(dest)) -> listOf<String>()
                mergeDelimiter != null -> listOf(listOfNotNull(prefix + names.first(), toString(prop.get(dest))).joinToString(mergeDelimiter))
                else -> listOfNotNull(prefix + names.first(), toString(prop.get(dest)))
            }

    open fun apply(s: String) = prop.set(dest, fromString(s))
}


class NullablePropMapper<C, V : Any?, out P : KMutableProperty1<C, V>>(dest: C,
                                                                       prop: P,
                                                                       names: List<String> = listOf(),
                                                                       fromString: ((String) -> V),
                                                                       toString: ((V) -> String?) = { it.toString() },
                                                                       skipIf: ((V) -> Boolean) = { it == null },
                                                                       mergeDelimiter: String? = null)
: PropMapper<C, V, P>(dest = dest, prop = prop, names = if (names.any()) names else listOf(prop.name),
                      fromString = fromString, toString = toString, skipIf = skipIf, mergeDelimiter = mergeDelimiter)


class StringPropMapper<C, out P : KMutableProperty1<C, String>>(dest: C,
                                                                prop: P,
                                                                names: List<String> = listOf(),
                                                                fromString: ((String) -> String) = { it },
                                                                toString: ((String) -> String?) = { it },
                                                                skipIf: ((String) -> Boolean) = String::isEmpty,
                                                                mergeDelimiter: String? = null)
: PropMapper<C, String, P>(dest = dest, prop = prop, names = if (names.any()) names else listOf(prop.name),
                           fromString = fromString, toString = toString, skipIf = skipIf, mergeDelimiter = mergeDelimiter)


class BoolPropMapper<C, out P : KMutableProperty1<C, Boolean>>(dest: C, prop: P, names: List<String> = listOf())
: PropMapper<C, Boolean, P>(dest = dest, prop = prop, names = if (names.any()) names else listOf(prop.name),
                            fromString = { true }, toString = { null }, skipIf = { !prop.get(dest) })


class RestPropMapper<C, out P : KMutableProperty1<C, MutableCollection<String>>>(dest: C, prop: P)
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
                val optionLength = prefix.length + matchingOption!!.length
                when {
                    propMapper is BoolPropMapper<*, *> -> {
                        if (param.length > optionLength)
                            throw IllegalArgumentException("Invalid switch option '$param', expecting $prefix$matchingOption without arguments")
                        propMapper.apply("")
                    }
                    param.length > optionLength ->
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


fun String.trimQuotes() = trim('"','\'')


interface OptionsGroup : Serializable {
    val mappers: List<PropMapper<*, *, *>>
}

fun Iterable<String>.filterExtractProps(vararg groups: OptionsGroup, prefix: String): Iterable<String> =
        filterExtractProps(groups.flatMap { it.mappers }, prefix)


data class DaemonJVMOptions(
        var maxMemory: String = "",
        var maxPermSize: String = "",
        var reservedCodeCacheSize: String = "",
        var jvmParams: MutableCollection<String> = arrayListOf()
) : OptionsGroup {

    override val mappers: List<PropMapper<*, *, *>>
        get() = listOf(StringPropMapper(this, DaemonJVMOptions::maxMemory, listOf("Xmx"), mergeDelimiter = ""),
                       StringPropMapper(this, DaemonJVMOptions::maxPermSize, listOf("XX:MaxPermSize"), mergeDelimiter = "="),
                       StringPropMapper(this, DaemonJVMOptions::reservedCodeCacheSize, listOf("XX:ReservedCodeCacheSize"), mergeDelimiter = "="),
                       restMapper)

    val restMapper: RestPropMapper<*, *>
        get() = RestPropMapper(this, DaemonJVMOptions::jvmParams)
}


data class DaemonOptions(
        var runFilesPath: String = COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH,
        var autoshutdownMemoryThreshold: Long = COMPILE_DAEMON_MEMORY_THRESHOLD_INFINITE,
        var autoshutdownIdleSeconds: Int = COMPILE_DAEMON_DEFAULT_IDLE_TIMEOUT_S,
        var autoshutdownUnusedSeconds: Int = COMPILE_DAEMON_DEFAULT_UNUSED_TIMEOUT_S,
        var shutdownDelayMilliseconds: Long = COMPILE_DAEMON_DEFAULT_SHUTDOWN_DELAY_MS,
        var forceShutdownTimeoutMilliseconds: Long = COMPILE_DAEMON_FORCE_SHUTDOWN_DEFAULT_TIMEOUT_MS,
        var verbose: Boolean = false,
        var reportPerf: Boolean = false
) : OptionsGroup {

    override val mappers: List<PropMapper<*, *, *>>
        get() = listOf(PropMapper(this, DaemonOptions::runFilesPath, fromString = String::trimQuotes),
                       PropMapper(this, DaemonOptions::autoshutdownMemoryThreshold, fromString = String::toLong, skipIf = { it == 0L }, mergeDelimiter = "="),
                // TODO: implement "use default" value without specifying default, so if client and server uses different defaults, it should not lead to many params in the cmd line; use 0 for it and used different val for infinite
                       PropMapper(this, DaemonOptions::autoshutdownIdleSeconds, fromString = String::toInt, skipIf = { it == 0 }, mergeDelimiter = "="),
                       PropMapper(this, DaemonOptions::autoshutdownUnusedSeconds, fromString = String::toInt, skipIf = { it == COMPILE_DAEMON_DEFAULT_UNUSED_TIMEOUT_S }, mergeDelimiter = "="),
                       PropMapper(this, DaemonOptions::shutdownDelayMilliseconds, fromString = String::toLong, skipIf = { it == COMPILE_DAEMON_DEFAULT_SHUTDOWN_DELAY_MS }, mergeDelimiter = "="),
                       PropMapper(this, DaemonOptions::forceShutdownTimeoutMilliseconds, fromString = String::toLong, skipIf = { it == COMPILE_DAEMON_FORCE_SHUTDOWN_DEFAULT_TIMEOUT_MS }, mergeDelimiter = "="),
                       BoolPropMapper(this, DaemonOptions::verbose),
                       BoolPropMapper(this, DaemonOptions::reportPerf))
}

// TODO: consider implementing generic approach to it or may be replace getters with ones returning default if necessary
val DaemonOptions.runFilesPathOrDefault: String
    get() = if (runFilesPath.isBlank()) COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH else runFilesPath


fun Iterable<String>.distinctStringsDigest(): ByteArray =
        MessageDigest.getInstance(CLASSPATH_ID_DIGEST)
                .digest(this.distinct().sorted().joinToString("").toByteArray())

fun ByteArray.toHexString(): String = joinToString("", transform = { "%02x".format(it) })


data class CompilerId(
        var compilerClasspath: List<String> = listOf(),
        var compilerVersion: String = ""
) : OptionsGroup {

    override val mappers: List<PropMapper<*, *, *>>
        get() = listOf(PropMapper(this, CompilerId::compilerClasspath, toString = { it.joinToString(File.pathSeparator) }, fromString = { it.trimQuotes().split(File.pathSeparator) }),
                       StringPropMapper(this, CompilerId::compilerVersion))

    companion object {
        @JvmStatic
        fun makeCompilerId(vararg paths: File): CompilerId = makeCompilerId(paths.asIterable())

        @JvmStatic
        fun makeCompilerId(paths: Iterable<File>): CompilerId =
                CompilerId(compilerClasspath = paths.map { it.absolutePath })
    }
}


fun isDaemonEnabled(): Boolean = System.getProperty(COMPILE_DAEMON_ENABLED_PROPERTY) != null

fun configureDaemonJVMOptions(opts: DaemonJVMOptions,
                              vararg additionalParams: String,
                              inheritMemoryLimits: Boolean,
                              inheritOtherJvmOptions: Boolean,
                              inheritAdditionalProperties: Boolean
): DaemonJVMOptions =
        configureDaemonJVMOptions(opts, additionalParams.asIterable(), inheritMemoryLimits, inheritOtherJvmOptions, inheritAdditionalProperties)

// TODO: expose sources for testability and test properly
fun configureDaemonJVMOptions(opts: DaemonJVMOptions,
                              additionalParams: Iterable<String>,
                              inheritMemoryLimits: Boolean,
                              inheritOtherJvmOptions: Boolean,
                              inheritAdditionalProperties: Boolean
): DaemonJVMOptions {
    // note: sequence matters, e.g. explicit override in COMPILE_DAEMON_JVM_OPTIONS_PROPERTY should be done after inputArguments processing
    if (inheritMemoryLimits || inheritOtherJvmOptions) {
        val jvmArguments = ManagementFactory.getRuntimeMXBean().inputArguments
        val targetOptions = if (inheritMemoryLimits) opts else DaemonJVMOptions()
        val otherArgs = jvmArguments.filterExtractProps(targetOptions.mappers, prefix = "-")

        if (inheritMemoryLimits && opts.maxMemory.isBlank()) {
            val maxMemBytes = Runtime.getRuntime().maxMemory()
            // rounding up
            val maxMemMegabytes = maxMemBytes / (1024 * 1024) + if (maxMemBytes % (1024 * 1024) == 0L) 0 else 1
            opts.maxMemory = "${maxMemMegabytes}m"
        }

        if (inheritOtherJvmOptions) {
            opts.jvmParams.addAll(
                    otherArgs.filterNot {
                        it.startsWith("agentlib") ||
                        it.startsWith("D" + COMPILE_DAEMON_LOG_PATH_PROPERTY) ||
                        it.startsWith("D" + KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY) ||
                        it.startsWith("D" + COMPILE_DAEMON_JVM_OPTIONS_PROPERTY) ||
                        it.startsWith("D" + COMPILE_DAEMON_OPTIONS_PROPERTY)
                    })
        }
    }
    System.getProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY)?.let {
        opts.jvmParams.addAll(
                it.trimQuotes()
                  .split("(?<!\\\\),".toRegex())  // using independent non-capturing group with negative lookahead zero length assertion to split only on non-escaped commas
                  .map { it.replace("\\\\(.)".toRegex(), "$1") } // de-escaping characters escaped by backslash, straightforward, without exceptions
                  .filterExtractProps(opts.mappers, "-", opts.restMapper))
    }

    // assuming that from the conflicting options the last one is taken
    // TODO: compare and override
    opts.jvmParams.addAll(additionalParams)

    if (inheritAdditionalProperties) {
        System.getProperty(COMPILE_DAEMON_LOG_PATH_PROPERTY)?.let { opts.jvmParams.add("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"$it\"") }
        System.getProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY)?.let { opts.jvmParams.add("D$KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY") }
    }
    return opts
}


fun configureDaemonJVMOptions(vararg additionalParams: String,
                              inheritMemoryLimits: Boolean,
                              inheritOtherJvmOptions: Boolean,
                              inheritAdditionalProperties: Boolean
): DaemonJVMOptions =
        configureDaemonJVMOptions(DaemonJVMOptions(),
                                  additionalParams = *additionalParams,
                                  inheritMemoryLimits = inheritMemoryLimits,
                                  inheritOtherJvmOptions = inheritOtherJvmOptions,
                                  inheritAdditionalProperties = inheritAdditionalProperties)


fun configureDaemonOptions(opts: DaemonOptions): DaemonOptions {
    System.getProperty(COMPILE_DAEMON_OPTIONS_PROPERTY)?.let {
        val unrecognized = it.trimQuotes().split(",").filterExtractProps(opts.mappers, "")
        if (unrecognized.any())
            throw IllegalArgumentException(
                    "Unrecognized daemon options passed via property $COMPILE_DAEMON_OPTIONS_PROPERTY: " + unrecognized.joinToString(" ") +
                    "\nSupported options: " + opts.mappers.joinToString(", ", transform = { it.names.first() }))
    }
    System.getProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY)?.let { opts.verbose = true }
    System.getProperty(COMPILE_DAEMON_REPORT_PERF_PROPERTY)?.let { opts.reportPerf = true }
    return opts
}


fun configureDaemonOptions(): DaemonOptions = configureDaemonOptions(DaemonOptions())


private val humanizedMemorySizeRegex = "(\\d+)([kmg]?)".toRegex()

private fun String.memToBytes(): Long? =
        humanizedMemorySizeRegex
            .matchEntire(this.trim().toLowerCase())
            ?.groups?.let { match ->
                match[1]?.value?.let {
                    it.toLong() *
                    when (match[2]?.value) {
                        "k" -> 1 shl 10
                        "m" -> 1 shl 20
                        "g" -> 1 shl 30
                        else -> 1
                    }
                }
            }


private val daemonJVMOptionsMemoryProps =
    listOf(DaemonJVMOptions::maxMemory, DaemonJVMOptions::maxPermSize, DaemonJVMOptions::reservedCodeCacheSize)

infix fun DaemonJVMOptions.memorywiseFitsInto(other: DaemonJVMOptions): Boolean =
        daemonJVMOptionsMemoryProps
            .all { (it.get(this).memToBytes() ?: 0) <= (it.get(other).memToBytes() ?: 0) }

fun compareDaemonJVMOptionsMemory(left: DaemonJVMOptions, right: DaemonJVMOptions): Int {
    val props = daemonJVMOptionsMemoryProps.map { Pair(it.get(left).memToBytes() ?: 0, it.get(right).memToBytes() ?: 0) }
    return when {
        props.all { it.first == it.second } -> 0
        props.all { it.first <= it.second } -> -1
        props.all { it.first >= it.second } -> 1
        else -> 0
    }
}

class DaemonJVMOptionsMemoryComparator : Comparator<DaemonJVMOptions> {
    override fun compare(left: DaemonJVMOptions, right: DaemonJVMOptions): Int = compareDaemonJVMOptionsMemory(left, right)
}


fun DaemonJVMOptions.updateMemoryUpperBounds(other: DaemonJVMOptions): DaemonJVMOptions {
    daemonJVMOptionsMemoryProps
        .forEach { if ((it.get(this).memToBytes() ?: 0) < (it.get(other).memToBytes() ?: 0)) it.set(this, it.get(other)) }
    return this
}
