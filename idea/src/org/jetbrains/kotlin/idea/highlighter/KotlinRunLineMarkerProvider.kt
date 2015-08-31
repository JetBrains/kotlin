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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.execution.lineMarker.RunLineMarkerInfo
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.JetIcons
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.JetNamedFunction


public class KotlinRunLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(e: PsiElement): LineMarkerInfo<PsiElement>? {
        val function = e.parent as? JetNamedFunction
        if (function == null) return null

        if (function.nameIdentifier != e) return null

        val detector = MainFunctionDetector { function ->
            function.resolveToDescriptor() as FunctionDescriptor
        }

        if (detector.isMain(function)) {
            return RunLineMarkerInfo(function, JetIcons.LAUNCH, null)
        }

        return null

    }

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
    }
}