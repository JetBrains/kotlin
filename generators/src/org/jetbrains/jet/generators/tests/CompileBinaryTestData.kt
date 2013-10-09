/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.tests

import java.io.File
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

var cwd = File("")
fun mv(from: String, to: String) = File(cwd, from).renameTo(File(cwd, to))
fun cp(from: String, to: String) = File(cwd, from).copyTo(File(cwd, to))
fun rm(path: String) = FileUtil.delete(File(cwd, path))

fun runProcess(cmd: String) {
    val process = Runtime.getRuntime().exec(cmd)
    process.waitFor()
    process.getInputStream()?.reader()?.forEachLine { println(it) }
    process.getErrorStream()?.reader()?.forEachLine { System.err.println(it) }
    if (process.exitValue() != 0) {
        System.err.println("Exit code ${process.exitValue()} was returned by: $cmd")
    }
}

fun jar(dest: String, src: String) {
    runProcess("jar cvf $cwd/$dest -C $cwd $src")
}

Retention(RetentionPolicy.RUNTIME)
private annotation class GenScript(val dir: String, val source: String, val binary: String)

private object BinaryTestData {
    GenScript("compiler/testData/compileKotlinAgainstBinariesCustom/brokenJarWithNoClassForObjectProperty", "source.kt", "broken.jar")
    fun genBrokenJarWithNoClassForObjectProperty() {
        rm("test/Lol.class")
        jar("broken.jar", "test")
        rm("test")
    }

    GenScript("compiler/testData/compileKotlinAgainstCustomBinaries/missingEnumReferencedInAnnotation", "MissingEnum.kt", "MissingEnum.jar")
    fun genMissingEnumReferencedInAnnotation() {
        rm("test/E.class")
        jar("MissingEnum.jar", "test")
        rm("test")
    }

    GenScript("idea/testData/completion/basic/custom/", "TopLevelNonImportedExtFunSource.kt", "TopLevelNonImportedExtFun.jar")
    fun genTopLevelNonImportedExtFun() {
        jar("TopLevelNonImportedExtFun.jar", "abc")
        rm("abc")
    }

    GenScript("idea/testData/completion/basic/custom/", "TopLevelNonImportedFunSource.kt", "TopLevelNonImportedFun.jar")
    fun genTopLevelNonImportedFun() {
        jar("TopLevelNonImportedFun.jar", "abc")
        rm("abc")
    }
}

fun changeDirectory(dir: String) {
    cwd = File(dir)
}

fun deleteBinary(file: String) = rm(file)

fun compileSource(src: String) {
    // We assume that ${script.source}.txt file exists and contains the source to compile. Kotlin compiler would not compile the source
    // with the ".txt" extension, so we temporarily rename this file to ${script.source} (it should end with ".kt")
    assert(src.endsWith(".kt"), "Source should have a '.kt' extension: $src")
    mv("$src.txt", src)
    try {
        runProcess("dist/kotlinc/bin/kotlinc-jvm${if (SystemInfo.isWindows) ".bat" else ""} -src $cwd/$src -output $cwd")
    }
    finally {
        mv(src, "$src.txt")
    }
}

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    for (method in javaClass<BinaryTestData>().getDeclaredMethods()) {
        val script = method.getAnnotation(javaClass<GenScript>()) as? GenScript ?: continue

        println()
        println("---------------------------------")
        println("Processing: ${script.dir}")

        changeDirectory(script.dir)
        deleteBinary(script.binary)
        compileSource(script.source)

        method.invoke(BinaryTestData)
    }
}
