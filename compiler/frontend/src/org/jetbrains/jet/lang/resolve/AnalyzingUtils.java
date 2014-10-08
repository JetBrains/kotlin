/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticSink;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid;
import org.jetbrains.jet.lang.psi.debugText.DebugTextPackage;

import java.util.ArrayList;
import java.util.List;

public class AnalyzingUtils {

    public abstract static class PsiErrorElementVisitor extends JetTreeVisitorVoid {
        @Override
        public abstract void visitErrorElement(PsiErrorElement element);
    }


    public static void checkForSyntacticErrors(@NotNull PsiElement root) {
        root.acceptChildren(new PsiErrorElementVisitor() {
            @Override
            public void visitErrorElement(PsiErrorElement element) {
                throw new IllegalArgumentException(element.getErrorDescription() + "; looking at " + element.getNode().getElementType() + " '" + element.getText() + DiagnosticUtils.atLocation(element));
            }
        });
    }
    
    public static List<PsiErrorElement> getSyntaxErrorRanges(@NotNull PsiElement root) {
        final ArrayList<PsiErrorElement> r = new ArrayList<PsiErrorElement>();
        root.acceptChildren(new PsiErrorElementVisitor() {
            @Override
            public void visitErrorElement(PsiErrorElement element) {
                r.add(element);
            }
        });
        return r;
    }

    public static void throwExceptionOnErrors(BindingContext bindingContext) {
        for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
            DiagnosticSink.THROW_EXCEPTION.report(diagnostic);
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------

    public static String formDebugNameForBindingTrace(@NotNull String debugName, @Nullable Object resolutionSubjectForMessage) {
        StringBuilder debugInfo = new StringBuilder(debugName);
        if (resolutionSubjectForMessage instanceof JetElement) {
            JetElement element = (JetElement) resolutionSubjectForMessage;
            debugInfo.append(" ").append(DebugTextPackage.getDebugText(element));
            debugInfo.append(" in ").append(element.getContainingFile().getName());
        }
        else if (resolutionSubjectForMessage != null) {
            debugInfo.append(" ").append(resolutionSubjectForMessage);
        }
        return debugInfo.toString();
    }
}
