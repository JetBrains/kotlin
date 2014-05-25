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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.KotlinLightClassForExplicitDeclaration;
import org.jetbrains.jet.asJava.KotlinLightClassForPackage;
import org.jetbrains.jet.lang.psi.JetClassBody;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.jet.plugin.project.TargetPlatform;
import org.jetbrains.jet.plugin.project.TargetPlatformDetector;

public class DuplicateJvmSignatureAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        Diagnostics diagnostics;
        
        PsiElement parent = element.getParent();
        if (parent instanceof JetFile) {
            if (TargetPlatformDetector.getPlatform((JetFile) parent) != TargetPlatform.JVM) return;
            diagnostics = getDiagnosticsForPackage((JetFile) parent);
        }
        else if (parent instanceof JetClassBody) {
            PsiElement parentsParent = parent.getParent();
            if (!(parentsParent instanceof JetClassOrObject)) return;

            if (TargetPlatformDetector.getPlatform(((JetClassBody) parent).getContainingJetFile()) != TargetPlatform.JVM) return;

            diagnostics = getDiagnosticsForNonLocalClass((JetClassOrObject) parentsParent);
        }
        else {
            return;
        }
        JetPsiChecker.annotateElement(element, holder, diagnostics);
    }

    @NotNull
    private static Diagnostics getDiagnosticsForPackage(JetFile file) {
        Project project = file.getProject();
        return KotlinLightClassForPackage.FileStubCache.getInstance(project).get(
                file.getPackageFqName(),
                GlobalSearchScope.allScope(project)
        ).getValue().getExtraDiagnostics();
    }

    @NotNull
    private static Diagnostics getDiagnosticsForNonLocalClass(JetClassOrObject jetClassOrObject) {
        return KotlinLightClassForExplicitDeclaration.getLightClassData(jetClassOrObject).getExtraDiagnostics();
    }
}
