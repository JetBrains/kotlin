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

package org.jetbrains.kotlin.idea.refactoring.move.moveFilesOrDirectories

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassHandler
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.classes.KtLightClass

class MoveKotlinClassHandler : MoveClassHandler {
    override fun doMoveClass(aClass: PsiClass, moveDestination: PsiDirectory): PsiClass? = null
    override fun getName(clazz: PsiClass?): String? = null

    /**
     * Ensure that Kotlin classes are not processed twice when moving Kotlin files
     * (once through the Java move handler calling KtFile.getClasses() and another time through the Kotlin move handler)
     */
    override fun preprocessUsages(results: MutableCollection<UsageInfo>) {
        results.removeAll { usageInfo ->
            usageInfo is MoveRenameUsageInfo && usageInfo.referencedElement is KtLightClass
        }
    }

    override fun prepareMove(aClass: PsiClass) {
    }

    override fun finishMoveClass(aClass: PsiClass) {
    }

}