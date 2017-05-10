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

package org.jetbrains.kotlin.idea.refactoring.move.changePackage

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.core.getFqNameByDirectory
import org.jetbrains.kotlin.idea.core.packageMatchesDirectory
import org.jetbrains.kotlin.idea.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.refactoring.hasIdentifiersOnly
import org.jetbrains.kotlin.idea.refactoring.isInjectedFragment
import org.jetbrains.kotlin.psi.KtPackageDirective

class ChangePackageToMatchDirectoryIntention : SelfTargetingOffsetIndependentIntention<KtPackageDirective>(
        KtPackageDirective::class.java, "", "Change file's package to match directory"
) {
    override fun isApplicableTo(element: KtPackageDirective): Boolean {
        val file = element.containingKtFile
        if (file.isInjectedFragment || file.packageMatchesDirectory()) return false

        val fqNameByDirectory = file.getFqNameByDirectory()
        text = if (!fqNameByDirectory.hasIdentifiersOnly()) {
            "File package doesn't match directory (cannot fix)"
        }
        else {
            "Change file's package to '${fqNameByDirectory.asString()}'"
        }
        return true
    }

    override fun applyTo(element: KtPackageDirective, editor: Editor?) {
        val file = element.containingKtFile
        val newFqName = file.getFqNameByDirectory()
        if (!newFqName.hasIdentifiersOnly()) return
        KotlinChangePackageRefactoring(file).run(newFqName)
    }
}
