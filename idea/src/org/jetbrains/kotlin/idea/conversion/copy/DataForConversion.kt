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

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import java.util.ArrayList

data class DataForConversion private(
        val elementsAndTexts: Collection<Any> /* list consisting of PsiElement's to convert and plain String's */,
        val importsAndPackage: String
) {
    companion object {
        fun prepare(copiedCode: CopiedJavaCode, project: Project): DataForConversion  {
            assert(copiedCode.startOffsets.size() == copiedCode.endOffsets.size(), "Must have the same size")

            val sourceFileText = copiedCode.fileText
            val sourceFile = PsiFileFactory.getInstance(project).createFileFromText(JavaLanguage.INSTANCE, sourceFileText) as PsiJavaFile
            val importsAndPackage = buildImportsAndPackage(sourceFile)

            val elementsAndTexts = ArrayList<Any>()
            for (i in copiedCode.startOffsets.indices) {
                elementsAndTexts.collectElementsToConvert(sourceFile, sourceFileText, TextRange(copiedCode.startOffsets[i], copiedCode.endOffsets[i]))
            }

            return DataForConversion(elementsAndTexts, importsAndPackage)
        }

        private fun MutableList<Any>.collectElementsToConvert(
                file: PsiJavaFile,
                fileText: String,
                range: TextRange
        ) {
            val elements = file.elementsInRange(range)
            if (elements.isEmpty()) {
                add(fileText.substring(range.getStartOffset(), range.getEndOffset()))
            }
            else {
                add(fileText.substring(range.getStartOffset(), elements.first().getTextRange().getStartOffset()))
                addAll(elements)
                add(fileText.substring(elements.last().getTextRange().getEndOffset(), range.getEndOffset()))
            }
        }

        private fun buildImportsAndPackage(sourceFile: PsiJavaFile): String {
            return StringBuilder {
                val packageName = sourceFile.getPackageName()
                if (!packageName.isEmpty()) {
                    append("package $packageName\n")
                }

                val importList = sourceFile.getImportList()
                if (importList != null) {
                    for (import in importList.getImportStatements()) {
                        val qualifiedName = import.getQualifiedName() ?: continue
                        if (import.isOnDemand()) {
                            append("import $qualifiedName.*\n")
                        }
                        else {
                            val fqName = FqNameUnsafe(qualifiedName)
                            // skip explicit imports of platform classes mapped into Kotlin classes
                            if (fqName.isSafe() && JavaToKotlinClassMap.INSTANCE.mapPlatformClass(fqName.toSafe()).isNotEmpty()) continue
                            append("import $qualifiedName\n")
                        }
                    }
                    //TODO: static imports
                }
            }.toString()
        }
    }
}
