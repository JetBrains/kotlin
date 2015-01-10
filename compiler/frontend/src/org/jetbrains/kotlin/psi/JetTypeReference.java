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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes.ANNOTATION;

/**
 * Type reference element.
 * Underlying token is {@link org.jetbrains.jet.JetNodeTypes#TYPE_REFERENCE}
 */
public class JetTypeReference extends JetElementImplStub<KotlinPlaceHolderStub<JetTypeReference>> {

    public JetTypeReference(@NotNull ASTNode node) {
        super(node);
    }

    public JetTypeReference(KotlinPlaceHolderStub<JetTypeReference> stub) {
        super(stub, JetStubElementTypes.TYPE_REFERENCE);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitTypeReference(this, data);
    }

    @NotNull
    public List<JetAnnotation> getAttributeAnnotations() {
        return getStubOrPsiChildrenAsList(ANNOTATION);
    }

    @Nullable
    public JetTypeElement getTypeElement() {
        return JetStubbedPsiUtil.getStubOrPsiChild(this, JetStubElementTypes.TYPE_ELEMENT_TYPES, JetTypeElement.ARRAY_FACTORY);
    }

    public List<JetAnnotationEntry> getAnnotations() {
        List<JetAnnotationEntry> answer = null;
        for (JetAnnotation annotation : getAttributeAnnotations()) {
            if (answer == null) answer = new ArrayList<JetAnnotationEntry>();
            answer.addAll(annotation.getEntries());
        }
        return answer != null ? answer : Collections.<JetAnnotationEntry>emptyList();
    }
}
