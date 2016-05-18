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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.JavaChangeInfo
import com.intellij.usageView.UsageInfo

class KotlinWrapperForJavaUsageInfos(
        val javaChangeInfo: JavaChangeInfo,
        val javaUsageInfos: Array<UsageInfo>,
        primaryMethod: PsiElement
): UsageInfo(primaryMethod) {
    override fun hashCode() = javaChangeInfo.method.hashCode()

    override fun equals(other: Any?): Boolean {
        return other === this || (other is KotlinWrapperForJavaUsageInfos && javaChangeInfo.method == other.javaChangeInfo.method)
    }
}
