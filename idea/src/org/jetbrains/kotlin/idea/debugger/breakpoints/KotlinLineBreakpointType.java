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

package org.jetbrains.kotlin.idea.debugger.breakpoints;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.JetFunction;

public class KotlinLineBreakpointType extends JavaLineBreakpointType {
    public KotlinLineBreakpointType() {
        super("kotlin-line", "Kotlin Line Breakpoints");
    }

    @Override
    public boolean matchesPosition(@NotNull LineBreakpoint<?> breakpoint, @NotNull SourcePosition position) {
        if (super.matchesPosition(breakpoint, position)) return true;

        PsiElement containingMethod = getContainingMethod(breakpoint);
        if (containingMethod == null) return false;
        return inTheMethod(position, containingMethod);
    }

    @Override
    @Nullable
    public PsiElement getContainingMethod(@NotNull LineBreakpoint<?> breakpoint) {
        SourcePosition position = breakpoint.getSourcePosition();
        if (position == null) return null;

        return getContainingMethod(position.getElementAt());
    }

    @Nullable
    public static PsiElement getContainingMethod(@Nullable PsiElement elem) {
        //noinspection unchecked
        return PsiTreeUtil.getParentOfType(elem, JetFunction.class);
    }

    public static boolean inTheMethod(@NotNull SourcePosition pos, @NotNull PsiElement method) {
        PsiElement elem = pos.getElementAt();
        if (elem == null) return false;
        return Comparing.equal(getContainingMethod(elem), method);
    }

    @Override
    public boolean canPutAt(@NotNull VirtualFile file, int line, @NotNull Project project) {
        return BreakpointTypeUtilsKt.canPutAt(file, line, project, getClass());
    }


}
