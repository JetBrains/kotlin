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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM

public class ImportInsertHelperImpl : ImportInsertHelper() {
    /**
     * Add import directive into the PSI tree for the given package.
     *
     * @param importFqn full name of the import
     * @param file File where directive should be added.
     */
    override fun addImportDirectiveIfNeeded(importFqn: FqName, file: JetFile) {
        val importPath = ImportPath(importFqn, false)

        optimizeImportsOnTheFly(file)

        if (needImport(importPath, file)) {
            writeImportToFile(importPath, file)
        }
    }

    override fun optimizeImportsOnTheFly(file: JetFile): Boolean {
        if (CodeInsightSettings.getInstance().OPTIMIZE_IMPORTS_ON_THE_FLY) {
            OptimizeImportsProcessor(file.getProject(), file).runWithoutProgress()
            return true
        }
        else {
            return false
        }
    }

    override fun writeImportToFile(importPath: ImportPath, file: JetFile) {
        val psiFactory = JetPsiFactory(file.getProject())
        if (file is JetCodeFragment) {
            val newDirective = psiFactory.createImportDirective(importPath)
            (file as JetCodeFragment).addImportsFromString(newDirective.getText())
            return
        }

        val importList = file.getImportList()
        if (importList != null) {
            val newDirective = psiFactory.createImportDirective(importPath)
            importList.add(psiFactory.createNewLine())
            importList.add(newDirective)
        }
        else {
            val newDirective = psiFactory.createImportDirectiveWithImportList(importPath)
            val packageDirective = file.getPackageDirective()
            if (packageDirective == null) {
                throw IllegalStateException("Scripts are not supported: " + file.getName())
            }

            packageDirective.getParent().addAfter(newDirective, packageDirective)
        }
    }

    /**
     * Check that import is useless.
     */
    private fun isImportedByDefault(importPath: ImportPath, jetFile: JetFile): Boolean {
        if (importPath.fqnPart().isRoot()) {
            return true
        }

        if (!importPath.isAllUnder() && !importPath.hasAlias()) {
            // Single element import without .* and alias is useless
            if (importPath.fqnPart().isOneSegmentFQN()) {
                return true
            }

            // There's no need to import a declaration from the package of current file
            if (jetFile.getPackageFqName() == importPath.fqnPart().parent()) {
                return true
            }
        }

        return isImportedWithDefault(importPath, jetFile)
    }

    override fun isImportedWithDefault(importPath: ImportPath, contextFile: JetFile): Boolean {

        val defaultImports = if (ProjectStructureUtil.isJsKotlinModule(contextFile))
            TopDownAnalyzerFacadeForJS.DEFAULT_IMPORTS
        else
            TopDownAnalyzerFacadeForJVM.DEFAULT_IMPORTS
        return importPath.isImported(defaultImports)
    }

    override fun needImport(fqName: FqName, file: JetFile): Boolean {
        return needImport(ImportPath(fqName, false), file)
    }

    override fun needImport(importPath: ImportPath, file: JetFile): Boolean {
        return needImport(importPath, file, file.getImportDirectives())
    }

    override fun needImport(importPath: ImportPath, file: JetFile, importDirectives: List<JetImportDirective>): Boolean {
        if (isImportedByDefault(importPath, file)) {
            return false
        }

        if (!importDirectives.isEmpty()) {
            // Check if import is already present
            for (directive in importDirectives) {
                val existentImportPath = directive.getImportPath()
                if (existentImportPath != null && importPath.isImported(existentImportPath)) {
                    return false
                }
            }
        }

        return true
    }
}
