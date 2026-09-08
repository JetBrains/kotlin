/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.Collections;
import java.util.List;

/**
 * Represents the {@code dynamic} type used in Kotlin/JS for interoperability.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val jsObj: dynamic = js("{}")
 * //         ^_____^
 * }</pre>
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtDynamicType extends KtElementImplStub<KotlinPlaceHolderStub<KtDynamicType>> implements KtTypeElement {
    @KtImplementationDetail
    public KtDynamicType(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtDynamicType(@NotNull KotlinPlaceHolderStub<KtDynamicType> stub) {
        super(stub, KtNodeTypes.DYNAMIC_TYPE);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitDynamicType(this, data);
    }

    /** Always empty: the {@code dynamic} type has no type arguments. */
    @NotNull
    @Override
    public List<KtTypeReference> getTypeArgumentsAsTypes() {
        return Collections.emptyList();
    }
}
