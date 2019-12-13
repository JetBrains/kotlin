/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.load.java.components.JavaAnnotationMapper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.*

class ImportableFqNameClassifier(private val file: KtFile) {
    private val preciseImports = HashSet<FqName>()
    private val preciseImportPackages = HashSet<FqName>()
    private val allUnderImports = HashSet<FqName>()
    private val excludedImports = HashSet<FqName>()

    init {
        for (import in file.importDirectives) {
            val importPath = import.importPath ?: continue
            val fqName = importPath.fqName
            when {
                importPath.isAllUnder -> allUnderImports.add(fqName)
                !importPath.hasAlias() -> {
                    preciseImports.add(fqName)
                    preciseImportPackages.add(fqName.parent())
                }
                else -> excludedImports.add(fqName)
                // TODO: support aliased imports in completion
            }
        }
    }

    enum class Classification {
        fromCurrentPackage,
        topLevelPackage,
        preciseImport,
        defaultImport,
        allUnderImport,
        siblingImported,
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
            isJavaClassNotToBeUsedInKotlin(fqName) -> Classification.notToBeUsedInKotlin

            fqName.parent() == file.packageFqName -> Classification.fromCurrentPackage

            ImportInsertHelper.getInstance(file.project).isImportedWithDefault(importPath, file) -> Classification.defaultImport

            isImportedWithPreciseImport(fqName) -> Classification.preciseImport

            isImportedWithAllUnderImport(fqName) -> Classification.allUnderImport

            hasPreciseImportFromPackage(fqName.parent()) -> Classification.siblingImported

            else -> Classification.notImported
        }
    }

    private fun isImportedWithPreciseImport(name: FqName) = name in preciseImports
    private fun isImportedWithAllUnderImport(name: FqName) = name.parent() in allUnderImports && name !in excludedImports
    private fun hasPreciseImportFromPackage(packageName: FqName) = packageName in preciseImportPackages
}

fun isJavaClassNotToBeUsedInKotlin(fqName: FqName): Boolean =
    JavaToKotlinClassMap.isJavaPlatformClass(fqName) || JavaAnnotationMapper.javaToKotlinNameMap[fqName] != null
