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

package org.jetbrains.jet.plugin.structureView;

import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.util.treeView.smartTree.NodeProvider;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.Arrays;
import java.util.Collection;

public class JetStructureViewModel extends StructureViewModelBase {
    private static final Collection<NodeProvider> NODE_PROVIDERS = Arrays.<NodeProvider>asList(
            new KotlinInheritedMembersNodeProvider());

    private static final Sorter[] sorters = new Sorter[] {Sorter.ALPHA_SORTER};

    public JetStructureViewModel(@NotNull JetFile jetFile) {
        super(jetFile, new JetStructureViewElement(jetFile));
        withSuitableClasses(JetDeclaration.class);
    }

    @NotNull
    @Override
    public Collection<NodeProvider> getNodeProviders() {
        return NODE_PROVIDERS;
    }

    @NotNull
    @Override
    public Sorter[] getSorters() {
        return sorters;
    }
}
