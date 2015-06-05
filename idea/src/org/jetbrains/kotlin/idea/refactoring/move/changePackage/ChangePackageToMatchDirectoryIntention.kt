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
import org.jetbrains.kotlin.idea.core.refactoring.canRefactor
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.psi.JetPackageDirective
import org.jetbrains.kotlin.idea.core.getFqNameByDirectory
import org.jetbrains.kotlin.idea.core.packageMatchesDirectory

public class ChangePackageToMatchDirectoryIntention : JetSelfTargetingOffsetIndependentIntention<JetPackageDirective>(
        javaClass(), "", "Change file's package to match directory"
) {
    override fun isApplicableTo(element: JetPackageDirective): Boolean {
        val file = element.getContainingJetFile()
        if (file.packageMatchesDirectory()) return false

        setText("Change file's package to '${file.getFqNameByDirectory().asString()}'")
        return true
    }

    override fun applyTo(element: JetPackageDirective, editor: Editor) {
        KotlinChangePackageRefactoring(element.getContainingJetFile()).run(element.getContainingFile().getFqNameByDirectory())
    }
}