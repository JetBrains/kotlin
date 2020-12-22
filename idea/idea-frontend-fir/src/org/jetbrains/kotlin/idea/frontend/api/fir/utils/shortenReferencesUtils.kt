/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath

private val SimpleImportPathComparator: Comparator<ImportPath> = compareBy(ImportPath::toString)

/**
 * This is a partial copy from `org.jetbrains.kotlin.idea.util.ImportInsertHelperImpl.Companion.addImport`.
 *
 * We want it as a copy because we do not yet care about imports ordering, so we do not need a fancy comparator.
 */
internal fun addImportToFile(
    project: Project,
    file: KtFile,
    fqName: FqName,
    allUnder: Boolean = false,
    alias: Name? = null
) {
    val importPath = ImportPath(fqName, allUnder, alias)

    val psiFactory = KtPsiFactory(project)
    if (file is KtCodeFragment) {
        val newDirective = psiFactory.createImportDirective(importPath)
        file.addImportsFromString(newDirective.text)
    }

    val importList = file.importList
        ?: error("Trying to insert import $fqName into a file ${file.name} of type ${file::class.java} with no import list.")

    val newDirective = psiFactory.createImportDirective(importPath)
    val imports = importList.imports
    if (imports.isEmpty()) { //TODO: strange hack
        importList.add(psiFactory.createNewLine())
        importList.add(newDirective)
    } else {
        val insertAfter = imports
            .lastOrNull {
                val directivePath = it.importPath

                directivePath != null && SimpleImportPathComparator.compare(directivePath, importPath) <= 0
            }

        importList.addAfter(newDirective, insertAfter)
    }
}
