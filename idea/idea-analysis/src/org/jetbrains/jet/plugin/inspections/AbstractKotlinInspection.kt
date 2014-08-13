/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.CustomSuppressableInspectionTool
import com.intellij.psi.PsiElement
import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.codeInspection.SuppressManager

public abstract class AbstractKotlinInspection: LocalInspectionTool(), CustomSuppressableInspectionTool {
    public override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction>? {
        return SuppressManager.getInstance()!!.createSuppressActions(HighlightDisplayKey.find(getShortName())!!)
    }

    public override fun isSuppressedFor(element: PsiElement): Boolean {
        return SuppressManager.getInstance()!!.isSuppressedFor(element, getID())
    }
}
