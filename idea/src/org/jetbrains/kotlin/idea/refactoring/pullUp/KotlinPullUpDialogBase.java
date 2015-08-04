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

package org.jetbrains.kotlin.idea.refactoring.pullUp;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.memberPullUp.PullUpDialogBase;
import com.intellij.refactoring.ui.AbstractMemberSelectionTable;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinOrJavaClassCellRenderer;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfoStorage;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetNamedDeclaration;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

// TODO: This is workaround which allows KotlinPullUpDialog to be compiled against both Java 6 and 8
public abstract class KotlinPullUpDialogBase extends
                                    PullUpDialogBase<KotlinMemberInfoStorage, KotlinMemberInfo, JetNamedDeclaration, PsiNamedElement> {
    protected KotlinPullUpDialogBase(
            Project project,
            JetClassOrObject object,
            List<PsiNamedElement> superClasses,
            KotlinMemberInfoStorage memberInfoStorage,
            String title
    ) {
        super(project, object, superClasses, memberInfoStorage, title);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initClassCombo(JComboBox classCombo) {
        classCombo.setRenderer(new KotlinOrJavaClassCellRenderer());
        classCombo.addItemListener(
                new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            if (myMemberSelectionPanel == null) return;
                            AbstractMemberSelectionTable<JetNamedDeclaration, KotlinMemberInfo> table = myMemberSelectionPanel.getTable();
                            if (table == null) return;
                            table.setMemberInfos(myMemberInfos);
                            table.fireExternalDataChange();
                        }
                    }
                }
        );
    }
}
