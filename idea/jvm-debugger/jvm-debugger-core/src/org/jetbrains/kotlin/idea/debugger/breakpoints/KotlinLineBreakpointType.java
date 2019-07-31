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
import com.intellij.debugger.engine.PositionManagerImpl;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.impl.breakpoints.XLineBreakpointImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;
import org.jetbrains.kotlin.idea.debugger.KotlinPositionManager;
import org.jetbrains.kotlin.psi.KtClassInitializer;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFunction;

import java.util.List;

public class KotlinLineBreakpointType extends JavaLineBreakpointType {
    public KotlinLineBreakpointType() {
        super("kotlin-line", "Kotlin Line Breakpoints");
    }

    @NotNull
    @Override
    public Breakpoint<JavaLineBreakpointProperties> createJavaBreakpoint(
            Project project, XBreakpoint<JavaLineBreakpointProperties> breakpoint
    ) {
        return new KotlinLineBreakpoint(project, breakpoint);
    }

    @Override
    public boolean matchesPosition(@NotNull LineBreakpoint<?> breakpoint, @NotNull SourcePosition position) {
        JavaBreakpointProperties properties = getProperties(breakpoint);
        if (properties == null || properties instanceof JavaLineBreakpointProperties) {
            if (position instanceof KotlinPositionManager.KotlinReentrantSourcePosition) {
                return false;
            }

            if (properties != null && ((JavaLineBreakpointProperties) properties).getLambdaOrdinal() == null) return true;

            PsiElement containingMethod = getContainingMethod(breakpoint);
            if (containingMethod == null) return false;
            return inTheMethod(position, containingMethod);
        }

        return true;
    }

    @Override
    @Nullable
    public PsiElement getContainingMethod(@NotNull LineBreakpoint<?> breakpoint) {
        SourcePosition position = breakpoint.getSourcePosition();
        if (position == null) return null;

        JavaBreakpointProperties properties = getProperties(breakpoint);
        if (properties instanceof JavaLineBreakpointProperties) {
            Integer ordinal = ((JavaLineBreakpointProperties) properties).getLambdaOrdinal();
            PsiElement lambda = getLambdaByOrdinal(position, ordinal);
            if (lambda != null) return lambda;
        }

        return getContainingMethod(position.getElementAt());
    }


    @Nullable
    private static JavaBreakpointProperties getProperties(@NotNull LineBreakpoint<?> breakpoint) {
        XBreakpoint<?> xBreakpoint = breakpoint.getXBreakpoint();
        return xBreakpoint != null ? (JavaBreakpointProperties) xBreakpoint.getProperties() : null;
    }

    @Nullable
    private static KtFunction getLambdaByOrdinal(SourcePosition position, Integer ordinal) {
        if (ordinal != null && ordinal >= 0) {
            List<KtFunction> lambdas = BreakpointTypeUtilsKt.getLambdasAtLineIfAny(position);
            if (lambdas.size() > ordinal) {
                return lambdas.get(ordinal);
            }
        }
        return null;
    }

    @Nullable
    public static PsiElement getContainingMethod(@Nullable PsiElement elem) {
        //noinspection unchecked
        return PsiTreeUtil.getParentOfType(elem, KtFunction.class, KtClassInitializer.class);
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

    @NotNull
    @Override
    public List<JavaBreakpointVariant> computeVariants(@NotNull Project project, @NotNull XSourcePosition position) {
        return BreakpointTypeUtilsKt.computeVariants(project, position, this);
    }

    @Nullable
    @Override
    public TextRange getHighlightRange(XLineBreakpoint<JavaLineBreakpointProperties> breakpoint) {
        JavaLineBreakpointProperties properties = breakpoint.getProperties();
        if (properties != null) {
            Integer ordinal = properties.getLambdaOrdinal();
            if (ordinal != null) {
                Breakpoint javaBreakpoint = BreakpointManager.getJavaBreakpoint(breakpoint);
                if (javaBreakpoint instanceof LineBreakpoint) {
                    SourcePosition position = ((LineBreakpoint) javaBreakpoint).getSourcePosition();
                    if (position != null) {
                        KtFunction lambda = getLambdaByOrdinal(position, ordinal);
                        if (lambda != null) {
                            return lambda.getTextRange();
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static SourcePosition createLineSourcePosition(@NotNull XLineBreakpointImpl breakpoint) {
        VirtualFile file = breakpoint.getFile();
        if (file != null) {
            PsiFile psiFile = PsiManager.getInstance(breakpoint.getProject()).findFile(file);
            if (psiFile != null) {
                return SourcePosition.createFromLine(psiFile, breakpoint.getLine());
            }
        }
        return null;
    }

    @Nullable
    @Override
    public XSourcePosition getSourcePosition(@NotNull XBreakpoint<JavaLineBreakpointProperties> breakpoint) {
        JavaBreakpointProperties javaBreakpointProperties = breakpoint.getProperties();
        if (javaBreakpointProperties != null) {
            Integer ordinal = ((JavaLineBreakpointProperties)javaBreakpointProperties).getLambdaOrdinal();
            SourcePosition sourcePosition = createLineSourcePosition((XLineBreakpointImpl)breakpoint);
            if (sourcePosition != null) {
                KtFunction function = getLambdaByOrdinal(sourcePosition, ordinal);
                if (function != null) {
                    KtElement bodyElement = function.getBodyExpression();
                    if (bodyElement != null) {
                        SourcePosition linePosition = SourcePosition.createFromElement(bodyElement);
                        return ReadAction.compute(() -> {
                            if (linePosition != null) {
                                return DebuggerUtilsEx.toXSourcePosition(new PositionManagerImpl.JavaSourcePosition(linePosition, ordinal));
                            }
                            return null;
                        });
                    }
                }
            }
        }

        return super.getSourcePosition(breakpoint);
    }
}
