/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl;

import com.intellij.psi.stubs.StubElement;
import org.jetbrains.kotlin.psi.KtBackingField;
import org.jetbrains.kotlin.psi.stubs.KotlinBackingFieldStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KotlinBackingFieldStubImpl extends KotlinStubBaseImpl<KtBackingField>
        implements KotlinBackingFieldStub {
    private final boolean hasInitializer;
    public KotlinBackingFieldStubImpl(StubElement<?> parent, boolean hasInitializer) {
        super(parent, KtStubElementTypes.BACKING_FIELD);
        this.hasInitializer = hasInitializer;
    }

    @Override
    public boolean getHasInitializer() {
        return hasInitializer;
    }
}
