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
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.List;

/**
 * Represents a bracketed group of annotation entries applied to the same use-site target, which avoids repeating the
 * target for each entry.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    @set:[Inject, Autowire]
 * // ^_____________________^
 * }</pre>
 * <p>
 * For a single annotation entry, see {@link KtAnnotationEntry}.
 */
public class KtAnnotation extends KtElementImplStub<KotlinPlaceHolderStub<KtAnnotation>> {

    public KtAnnotation(@NotNull ASTNode node) {
        super(node);
    }

    public KtAnnotation(KotlinPlaceHolderStub<KtAnnotation> stub) {
        super(stub, KtStubBasedElementTypes.ANNOTATION);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitAnnotation(this, data);
    }

    public List<KtAnnotationEntry> getEntries() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.ANNOTATION_ENTRY);
    }

    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtAnnotationUseSiteTarget getUseSiteTarget() {
        return getStubOrPsiChild(KtStubBasedElementTypes.ANNOTATION_TARGET);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.removeAnnotationEntry(this, entry)}
     * instead.
     */
    @Deprecated
    public void removeEntry(@NotNull KtAnnotationEntry entry) {
        KtPsiMutationService.getInstance().removeAnnotationEntry(this, entry);
    }
}
