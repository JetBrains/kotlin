/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.tests

import com.intellij.openapi.util.Ref
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.regex.Pattern

private val FILE_NAME_ANNOTATIONS = arrayOf("@file:JvmName", "@file:kotlin.jvm.JvmName")

private val packagePattern = Pattern.compile("(?m)^\\s*package[ |\t]+([\\w|\\.]*)")

private val importPattern = Pattern.compile("import[ |\t]([\\w|]*\\.)")

internal fun genFiles(file: File, fileContent: String, filesHolder: CodegenTestsOnAndroidGenerator.FilesWriter): FqName? {
    val testFiles = createTestFiles(file, fileContent)
    if (testFiles.filter { it.name.endsWith(".java") }.isNotEmpty()) {
        //TODO support java files
        return null;
    }
    val ktFiles = testFiles.filter { it.name.endsWith(".kt") }
    if (ktFiles.isEmpty()) return null

    val newPackagePrefix = file.path.replace("\\\\|-|\\.|/".toRegex(), "_")
    val oldPackage = Ref<FqName>()
    val isSingle = testFiles.size == 1
    val resultFiles = testFiles.map {
        val fileName = if (isSingle) it.name else file.name.substringBeforeLast(".kt") + "/" + it.name
        TestClassInfo(
                fileName,
                changePackage(newPackagePrefix, it.content, oldPackage),
                oldPackage.get(),
                getGeneratedClassName(File(fileName), it.content, newPackagePrefix, oldPackage.get())
        )
    }

    /*replace all Class.forName*/
    resultFiles.forEach {
        file ->
        file.content = resultFiles.fold(file.content) { r, param ->
            patchClassForName(param.newClassId, param.oldPackage, r)
        }
    }

    /*patch imports and self imports*/
    resultFiles.forEach {
        file ->
        file.content = resultFiles.fold(file.content) { r, param ->
            r.patchImports(param.oldPackage, param.newPackage)
        }.patchSelfImports(file.newPackage)
    }

    resultFiles.forEach { resultFile ->
        if (resultFile.name.endsWith(".kt") || resultFile.name.endsWith(".kts")) {
            filesHolder.addFile(resultFile.name, resultFile.content)
        }
    }

    val boxFiles = resultFiles.filter { hasBoxMethod(it.content) }
    if (boxFiles.size != 1) {
        println("Several box methods in $file")
    }
    return boxFiles.last().newClassId
}


private fun createTestFiles(file: File, expectedText: String): List<CodegenTestCase.TestFile> {
    val files = KotlinTestUtils.createTestFiles(file.name, expectedText, object : KotlinTestUtils.TestFileFactoryNoModules<CodegenTestCase.TestFile>() {
        override fun create(fileName: String, text: String, directives: Map<String, String>): CodegenTestCase.TestFile {
            return CodegenTestCase.TestFile(fileName, text)
        }
    })
    return files
}

private fun hasBoxMethod(text: String): Boolean {
    return text.contains("fun box()")
}

class TestClassInfo(val name: String, var content: String, val oldPackage: FqName, val newClassId: FqName) {
    val newPackage = newClassId.parent()
}


private fun changePackage(newPackagePrefix: String, text: String, oldPackage: Ref<FqName>): String {
    val matcher = packagePattern.matcher(text)
    if (matcher.find()) {
        val oldPackageName = matcher.toMatchResult().group(1)
        oldPackage.set(FqName(oldPackageName))
        return matcher.replaceAll("package $newPackagePrefix.$oldPackageName")
    }
    else {
        oldPackage.set(FqName.ROOT)
        val packageDirective = "package $newPackagePrefix;\n"
        if (text.contains("@file:")) {
            val index = text.lastIndexOf("@file:")
            val packageDirectiveIndex = text.indexOf("\n", index)
            return text.substring(0, packageDirectiveIndex + 1) + packageDirective + text.substring(packageDirectiveIndex + 1)
        }
        else {
            return packageDirective + text
        }
    }
}

private fun getGeneratedClassName(file: File, text: String, newPackagePrefix: String, oldPackage: FqName): FqName {
    //TODO support multifile facades
    var packageFqName = FqName(newPackagePrefix)
    if (!oldPackage.isRoot) {
        packageFqName = packageFqName.child(Name.identifier(oldPackage.asString()))
    }
    for (annotation in FILE_NAME_ANNOTATIONS) {
        if (text.contains(annotation)) {
            val indexOf = text.indexOf(annotation)
            val annotationParameter = text.substring(text.indexOf("(\"", indexOf) + 2, text.indexOf("\")", indexOf))
            return packageFqName.child(Name.identifier(annotationParameter))
        }
    }

    return PackagePartClassUtils.getPackagePartFqName(packageFqName, file.name)
}

private fun patchClassForName(className: FqName, oldPackage: FqName, text: String): String {
    return text.replace(("Class\\.forName\\(\"" + oldPackage.child(className.shortName()).asString() + "\"\\)").toRegex(), "Class.forName(\"" + className.asString() + "\")")
}

private fun String.patchImports(oldPackage: FqName, newPackage: FqName): String {
    if (oldPackage.isRoot) return this

    return this.replace(("import\\s+" + oldPackage.asString()).toRegex(), "import " + newPackage.asString())
}


private fun String.patchSelfImports(newPackage: FqName): String {
    var newText = this;
    val matcher = importPattern.matcher(this)
    while (matcher.find()) {
        val possibleSelfImport = matcher.toMatchResult().group(1)
        val classOrObjectPattern = Pattern.compile("[\\s|^](class|object)\\s$possibleSelfImport[\\s|\\(|{|;|:]")
        if (classOrObjectPattern.matcher(newText).find()) {
            newText = newText.replace("import " + possibleSelfImport, "import " + newPackage.child(Name.identifier(possibleSelfImport)).asString())
        }
    }
    return newText
}
