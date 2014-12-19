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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo;

public abstract class JetUsageInfo<T extends PsiElement> extends UsageInfo {
    public JetUsageInfo(@NotNull T element) {
        super(element);
    }

    public JetUsageInfo(@NotNull PsiReference reference) {
        super(reference);
    }

    @Nullable
    @Override
    public T getElement() {
        //noinspection unchecked
        return (T) super.getElement();
    }

    public abstract boolean processUsage(JetChangeInfo changeInfo, T element);
}
