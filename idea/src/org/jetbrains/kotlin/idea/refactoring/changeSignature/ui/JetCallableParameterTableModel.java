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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.ui;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.changeSignature.ParameterTableModelBase;
import com.intellij.refactoring.changeSignature.ParameterTableModelItemBase;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetMethodDescriptor;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetValVar;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetPsiFactory;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public abstract class JetCallableParameterTableModel extends ParameterTableModelBase<JetParameterInfo, ParameterTableModelItemBase<JetParameterInfo>> {
    private final Project project;
    private final JetMethodDescriptor methodDescriptor;

    protected JetCallableParameterTableModel(JetMethodDescriptor methodDescriptor, PsiElement context, ColumnInfo... columnInfos) {
        super(context, context, columnInfos);
        this.methodDescriptor = methodDescriptor;
        project = context.getProject();
    }

    @Nullable
    public JetParameterInfo getReceiver() {
        return null;
    }

    @Override
    protected ParameterTableModelItemBase<JetParameterInfo> createRowItem(@Nullable JetParameterInfo parameterInfo) {
        if (parameterInfo == null) {
            parameterInfo = new JetParameterInfo(methodDescriptor.getBaseDescriptor(), -1, "", null, null, null, JetValVar.None, null);
        }
        JetPsiFactory psiFactory = JetPsiFactory(project);
        PsiCodeFragment paramTypeCodeFragment = psiFactory.createTypeCodeFragment(parameterInfo.getTypeText(), myTypeContext);
        JetExpression defaultValueForCall = parameterInfo.getDefaultValueForCall();
        PsiCodeFragment defaultValueCodeFragment = psiFactory.createExpressionCodeFragment(
                defaultValueForCall != null ? defaultValueForCall.getText() : "",
                myDefaultValueContext
        );
        return new ParameterTableModelItemBase<JetParameterInfo>(parameterInfo, paramTypeCodeFragment, defaultValueCodeFragment) {
            @Override
            public boolean isEllipsisType() {
                return false;
            }
        };
    }

    public static boolean isTypeColumn(ColumnInfo column) {
        return column instanceof TypeColumn;
    }

    public static boolean isNameColumn(ColumnInfo column) {
        return column instanceof NameColumn;
    }

    public static boolean isDefaultValueColumn(ColumnInfo column) {
        return column instanceof DefaultValueColumn;
    }
}
