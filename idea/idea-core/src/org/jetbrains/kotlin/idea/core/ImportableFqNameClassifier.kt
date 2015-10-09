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

package org.jetbrains.kotlin.idea.core

import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.load.java.components.JavaAnnotationMapper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.*

class ImportableFqNameClassifier(private val file: JetFile) {
    private val preciseImports = HashSet<FqName>()
    private val preciseImportPackages = HashSet<FqName>()
    private val allUnderImports = HashSet<FqName>()

    init {
        for (import in file.getImportDirectives()) {
            val importPath = import.getImportPath() ?: continue
            val fqName = importPath.fqnPart()
            if (importPath.isAllUnder()) {
                allUnderImports.add(fqName)
            }
            else {
                preciseImports.add(fqName)
                preciseImportPackages.add(fqName.parent())
            }
        }
    }

    enum class Classification {
        fromCurrentPackage,
        topLevelPackage,
        defaultImport,
        preciseImport,
        allUnderImport,
        hasImportFromSamePackage,
        notImported,
        notToBeUsedInKotlin
    }

    fun classify(fqName: FqName, isPackage: Boolean): Classification {
        val importPath = ImportPath(fqName, false)

        if (isPackage) {
            return when {
                isImportedWithPreciseImport(fqName) -> Classification.preciseImport
                fqName.parent().isRoot -> Classification.topLevelPackage
                else -> Classification.notImported
            }
        }

        return when {
            JavaToKotlinClassMap.INSTANCE.mapPlatformClass(fqName).isNotEmpty()
                    || JavaAnnotationMapper.javaToKotlinNameMap[fqName] != null -> Classification.notToBeUsedInKotlin

            fqName.parent() == file.packageFqName -> Classification.fromCurrentPackage

            ImportInsertHelper.getInstance(file.project).isImportedWithDefault(importPath, file) -> Classification.defaultImport

            isImportedWithPreciseImport(fqName) -> Classification.preciseImport

            isImportedWithAllUnderImport(fqName) -> Classification.allUnderImport

            hasPreciseImportFromPackage(fqName.parent()) -> Classification.hasImportFromSamePackage

            else -> Classification.notImported
        }
    }

    private fun isImportedWithPreciseImport(name: FqName) = name in preciseImports
    private fun isImportedWithAllUnderImport(name: FqName) = name.parent() in allUnderImports
    private fun hasPreciseImportFromPackage(packageName: FqName) = packageName in preciseImportPackages
}