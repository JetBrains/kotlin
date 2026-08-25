/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.ReplaceWith;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a bracketed group of annotation entries applied to the same use-site target, which avoids repeating the target for each entry.
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
    /** A shared empty array, which can be reused to avoid unnecessary allocations. */
    public static final KtAnnotation[] EMPTY_ARRAY = new KtAnnotation[0];

    @KtImplementationDetail
    public KtAnnotation(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtAnnotation(KotlinPlaceHolderStub<KtAnnotation> stub) {
        super(stub, KtNodeTypes.ANNOTATION);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitAnnotation(this, data);
    }

    /** Returns the individual annotation entries in this bracketed group, in source order. */
    public List<KtAnnotationEntry> getEntries() {
        return Arrays.asList(getStubOrPsiChildren(KtNodeTypes.ANNOTATION_ENTRY, KtAnnotationEntry.EMPTY_ARRAY));
    }

    /**
     * Returns the shared use-site target applied to all entries in this group (as in {@code @set:[...]}), or {@code null} if no use-site
     * target is specified.
     */
    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtAnnotationUseSiteTarget getUseSiteTarget() {
        return getStubOrPsiChild(KtStubBasedElementTypes.ANNOTATION_TARGET);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.removeAnnotationEntry(this, entry)}
     * instead.
     */
    @kotlin.Deprecated(
            message = "Use 'org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.removeAnnotationEntry(this, entry)' instead.",
            replaceWith = @ReplaceWith(
                    expression = "this.removeAnnotationEntry(entry)",
                    imports = "org.jetbrains.kotlin.idea.base.psi.removeAnnotationEntry"
            )
    )
    @Deprecated
    public void removeEntry(@NotNull KtAnnotationEntry entry) {
        KtPsiMutationService.getInstance().removeAnnotationEntry(this, entry);
    }
}
