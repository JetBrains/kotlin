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


fun<C, V, P: KProperty1<C,V>> C.propToParams(p: P, conv: ((v: V) -> String) = { it.toString() } ) =
        listOf("--daemon-" + p.name, conv(p.get(this)))

open class PropParser<C, V, P: KMutableProperty1<C, V>>(val dest: C, val prop: P, val parse: (s: String) -> V) {
    fun apply(s: String) = prop.set(dest, parse(s))
}

class BoolPropParser<C, P: KMutableProperty1<C, Boolean>>(dest: C, prop: P): PropParser<C, Boolean, P>(dest, prop, { true })

fun Iterable<String>.propParseFilter(parsers: List<PropParser<*,*,*>>) : Iterable<String>  {
    var currentParser: PropParser<*,*,*>? = null
    return filter { param ->
        if (currentParser == null) {
            currentParser = parsers.find { param.equals("--daemon-" + it.prop.name) }
            if (currentParser != null) {
                if (currentParser is BoolPropParser<*,*>) {
                    currentParser!!.apply("")
                    currentParser = null
                }
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
}

// TODO: find out how to create more generic variant using first constructor
//fun<C> C.propsToParams() {
//    val kc = C::class
//    kc.constructors.first().
//}

public interface CmdlineParams : Serializable {
    public val asParams: Iterable<String>
    public val parsers: List<PropParser<*,*,*>>
}

public fun Iterable<String>.propParseFilter(vararg cs: CmdlineParams) : Iterable<String> =
    propParseFilter(cs.flatMap { it.parsers })


public data class DaemonOptions(
        public var port: Int = COMPILE_DAEMON_DEFAULT_PORT,
        public var autoshutdownMemoryThreshold: Long = 0 /* 0 means unchecked */,
        public var autoshutdownIdleSeconds: Int = 0 /* 0 means unchecked */,
        public var startEcho: String = COMPILER_SERVICE_RMI_NAME
) : CmdlineParams {

    override val asParams: Iterable<String>
        get() =
            propToParams(::port) +
            propToParams(::autoshutdownMemoryThreshold) +
            propToParams(::autoshutdownIdleSeconds) +
            propToParams(::startEcho)

    override val parsers: List<PropParser<*,*,*>>
            get() = listOf( PropParser(this, ::port, { it.toInt()}),
                            PropParser(this, ::autoshutdownMemoryThreshold, { it.toLong()}),
                            PropParser(this, ::autoshutdownIdleSeconds, { it.toInt()}),
                            PropParser(this, ::startEcho, { it.trim('"') }))
}


val COMPILER_ID_DIGEST = "MD5"


fun updateSingleFileDigest(file: File, md: MessageDigest) {
    val stream = DigestInputStream(file.inputStream(), md)
    val buf = ByteArray(1024)
    while (stream.read(buf) == buf.size()) {}
    stream.close()
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

    override val asParams: Iterable<String>
        get() =
            propToParams(::compilerClasspath, { it.joinToString(File.pathSeparator) }) +
            propToParams(::compilerDigest) +
            propToParams(::compilerVersion)

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
                // TODO consider reading version and calculating checksum here
                CompilerId(compilerClasspath = paths.map { it.absolutePath }, compilerDigest = paths.getFilesClasspathDigest())
    }
}


