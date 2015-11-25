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

import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import java.io.File
import java.io.Serializable
import java.lang.management.ManagementFactory
import java.security.MessageDigest
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.text.RegexOption


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
public val COMPILE_DAEMON_TIMEOUT_INFINITE_S: Int = 0
public val COMPILE_DAEMON_DEFAULT_IDLE_TIMEOUT_S: Int = 7200 // 2 hours
public val COMPILE_DAEMON_DEFAULT_UNUSED_TIMEOUT_S: Int = 60
public val COMPILE_DAEMON_DEFAULT_SHUTDOWN_DELAY_MS: Long = 1000L // 1 sec
public val COMPILE_DAEMON_MEMORY_THRESHOLD_INFINITE: Long = 0L
public val COMPILE_DAEMON_FORCE_SHUTDOWN_DEFAULT_TIMEOUT_MS: Long = 10000L // 10 secs
public val COMPILE_DAEMON_TIMEOUT_INFINITE_MS: Long = 0L

public val COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH: String get() =
    FileSystem.getRuntimeStateFilesPath("kotlin", "daemon")

val CLASSPATH_ID_DIGEST = "MD5"


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
                mergeDelimiter != null -> listOf(listOfNotNull(prefix + names.first(), toString(prop.get(dest))).joinToString(mergeDelimiter))
                else -> listOfNotNull(prefix + names.first(), toString(prop.get(dest)))
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
        get() = listOf(StringPropMapper(this, DaemonJVMOptions::maxMemory, listOf("Xmx"), mergeDelimiter = ""),
                       StringPropMapper(this, DaemonJVMOptions::maxPermSize, listOf("XX:MaxPermSize"), mergeDelimiter = "="),
                       StringPropMapper(this, DaemonJVMOptions::reservedCodeCacheSize, listOf("XX:ReservedCodeCacheSize"), mergeDelimiter = "="),
                       restMapper)

    val restMapper: RestPropMapper<*, *>
        get() = RestPropMapper(this, DaemonJVMOptions::jvmParams)
}


public data class DaemonOptions(
        public var runFilesPath: String = COMPILE_DAEMON_DEFAULT_RUN_DIR_PATH,
        public var autoshutdownMemoryThreshold: Long = COMPILE_DAEMON_MEMORY_THRESHOLD_INFINITE,
        public var autoshutdownIdleSeconds: Int = COMPILE_DAEMON_DEFAULT_IDLE_TIMEOUT_S,
        public var autoshutdownUnusedSeconds: Int = COMPILE_DAEMON_DEFAULT_UNUSED_TIMEOUT_S,
        public var shutdownDelayMilliseconds: Long = COMPILE_DAEMON_DEFAULT_SHUTDOWN_DELAY_MS,
        public var forceShutdownTimeoutMilliseconds: Long = COMPILE_DAEMON_FORCE_SHUTDOWN_DEFAULT_TIMEOUT_MS,
        public var verbose: Boolean = false,
        public var reportPerf: Boolean = false
) : OptionsGroup {

    override val mappers: List<PropMapper<*, *, *>>
        get() = listOf(PropMapper(this, DaemonOptions::runFilesPath, fromString = { it.trimQuotes() }),
                       PropMapper(this, DaemonOptions::autoshutdownMemoryThreshold, fromString = { it.toLong() }, skipIf = { it == 0L }, mergeDelimiter = "="),
                // TODO: implement "use default" value without specifying default, so if client and server uses different defaults, it should not lead to many params in the cmd line; use 0 for it and used different val for infinite
                       PropMapper(this, DaemonOptions::autoshutdownIdleSeconds, fromString = { it.toInt() }, skipIf = { it == 0 }, mergeDelimiter = "="),
                       PropMapper(this, DaemonOptions::autoshutdownUnusedSeconds, fromString = { it.toInt() }, skipIf = { it == COMPILE_DAEMON_DEFAULT_UNUSED_TIMEOUT_S }, mergeDelimiter = "="),
                       PropMapper(this, DaemonOptions::shutdownDelayMilliseconds, fromString = { it.toLong() }, skipIf = { it == COMPILE_DAEMON_DEFAULT_SHUTDOWN_DELAY_MS }, mergeDelimiter = "="),
                       PropMapper(this, DaemonOptions::forceShutdownTimeoutMilliseconds, fromString = { it.toLong() }, skipIf = { it == COMPILE_DAEMON_FORCE_SHUTDOWN_DEFAULT_TIMEOUT_MS }, mergeDelimiter = "="),
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


public data class CompilerId(
        public var compilerClasspath: List<String> = listOf(),
        public var compilerVersion: String = ""
) : OptionsGroup {

    override val mappers: List<PropMapper<*, *, *>>
        get() = listOf(PropMapper(this, CompilerId::compilerClasspath, toString = { it.joinToString(File.pathSeparator) }, fromString = { it.trimQuotes().split(File.pathSeparator) }),
                       StringPropMapper(this, CompilerId::compilerVersion))

    companion object {
        @JvmStatic
        public fun makeCompilerId(vararg paths: File): CompilerId = makeCompilerId(paths.asIterable())

        @JvmStatic
        public fun makeCompilerId(paths: Iterable<File>): CompilerId =
                CompilerId(compilerClasspath = paths.map { it.absolutePath })
    }
}


public fun isDaemonEnabled(): Boolean = System.getProperty(COMPILE_DAEMON_ENABLED_PROPERTY) != null


public fun configureDaemonJVMOptions(opts: DaemonJVMOptions,
                                     vararg additionalParams: String,
                                     inheritMemoryLimits: Boolean,
                                     inheritAdditionalProperties: Boolean
): DaemonJVMOptions {
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

    opts.jvmParams.addAll(additionalParams)
    if (inheritAdditionalProperties) {
        System.getProperty(COMPILE_DAEMON_LOG_PATH_PROPERTY)?.let { opts.jvmParams.add("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"$it\"") }
        System.getProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY)?.let { opts.jvmParams.add("D$KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY") }
    }
    return opts
}


public fun configureDaemonJVMOptions(vararg additionalParams: String,
                                     inheritMemoryLimits: Boolean,
                                     inheritAdditionalProperties: Boolean
): DaemonJVMOptions =
        configureDaemonJVMOptions(DaemonJVMOptions(),
                                  additionalParams = *additionalParams,
                                  inheritMemoryLimits = inheritMemoryLimits,
                                  inheritAdditionalProperties = inheritAdditionalProperties)


public fun configureDaemonOptions(opts: DaemonOptions): DaemonOptions {
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


public fun configureDaemonOptions(): DaemonOptions = configureDaemonOptions(DaemonOptions())


fun String.memToBytes(): Long? =
        "(\\d+)([kmg]?)".toRegex()
                .matchEntire(this.trim().toLowerCase())
                ?.groups?.let { match ->
                    match.get(1)?.value?.let {
                        it.toLong() *
                        when (match.get(2)?.value) {
                            "k" -> 1 shl 10
                            "m" -> 1 shl 20
                            "g" -> 1 shl 30
                            else -> 1
                        }
                    }
                }


private val daemonJVMOptionsMemoryProps: List<KMutableProperty1<DaemonJVMOptions, String>> by lazy {
    listOf(DaemonJVMOptions::maxMemory, DaemonJVMOptions::maxPermSize, DaemonJVMOptions::reservedCodeCacheSize)
}

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
