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

package org.jetbrains.kotlin.idea.liveTemplates.macro;

import com.intellij.codeInsight.template.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.psi.JetFunction;
import org.jetbrains.kotlin.psi.JetParameter;

import java.util.ArrayList;
import java.util.List;

public class JetFunctionParametersMacro extends Macro {
    @Override
    public String getName() {
        return "functionParameters";
    }

    @Override
    public String getPresentableName() {
        return JetBundle.message("macro.fun.parameters");
    }

    @Override
    public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
        Project project = context.getProject();
        int templateStartOffset = context.getTemplateStartOffset();
        int offset = templateStartOffset > 0 ? context.getTemplateStartOffset() - 1 : context.getTemplateStartOffset();

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(context.getEditor().getDocument());
        if (file == null) return null;
        PsiElement place = file.findElementAt(offset);
        while (place != null){
            if (place instanceof JetFunction) {
                List<Result> result = new ArrayList<Result>();
                for (JetParameter param : ((JetFunction) place).getValueParameters()) {
                    String name = param.getName();
                    assert name != null;
                    result.add(new TextResult(name));
                }
                return new ListResult(result);
            }
            place = place.getParent();
        }
        return null;
    }

    @Override
    public boolean isAcceptableInContext(TemplateContextType context) {
        return context instanceof JavaCodeContextType;
    }

}
