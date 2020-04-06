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

package org.jetbrains.kotlin.idea.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class KtFileTreeNode extends PsiFileNode {
    public KtFileTreeNode(Project project, KtFile value, ViewSettings viewSettings) {
        super(project, value, viewSettings);
    }

    public final KtFile getKtFile() {
        return (KtFile) getValue();
    }

    @Override
    public Collection<AbstractTreeNode<?>> getChildrenImpl() {
        KtFile file = (KtFile) getValue();

        if (file == null) return Collections.emptyList();
        ArrayList<AbstractTreeNode<?>> result = new ArrayList<>();

        if (getSettings().isShowMembers()) {
            @SuppressWarnings("ConstantConditions") List<KtDeclaration> declarations = (file.isScript() ? file.getScript() : file).getDeclarations();

            for (KtDeclaration declaration : declarations) {
                if (declaration instanceof KtClassOrObject) {
                    result.add(new KtClassOrObjectTreeNode(file.getProject(), (KtClassOrObject) declaration, getSettings()));
                }
                else if (getSettings().isShowMembers()) {
                    result.add(new KtDeclarationTreeNode(getProject(), declaration, getSettings()));
                }
            }
        }

        return result;
    }
}
