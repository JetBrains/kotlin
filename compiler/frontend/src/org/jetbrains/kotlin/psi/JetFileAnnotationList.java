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

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

import java.util.List;

public class JetFileAnnotationList extends JetElementImplStub<KotlinPlaceHolderStub<JetFileAnnotationList>> {

    public JetFileAnnotationList(@NotNull ASTNode node) {
        super(node);
    }

    public JetFileAnnotationList(@NotNull KotlinPlaceHolderStub<JetFileAnnotationList> stub) {
        super(stub, JetStubElementTypes.FILE_ANNOTATION_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitFileAnnotationList(this, data);
    }

    @NotNull
    public List<JetAnnotation> getAnnotations() {
        return getStubOrPsiChildrenAsList(JetStubElementTypes.ANNOTATION);
    }

    @NotNull
    public List<JetAnnotationEntry> getAnnotationEntries() {
        return KotlinPackage.flatMap(getAnnotations(), new Function1<JetAnnotation, List<JetAnnotationEntry>>() {
            @Override
            public List<JetAnnotationEntry> invoke(JetAnnotation annotation) {
                return annotation.getEntries();
            }
        });
    }
}
