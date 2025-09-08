/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a Kotlin declaration that semantically has a return type (ex. function, property, parameter, and so on).
 * The return type itself may not be present in the source code in case it is inferred.
 */
public interface KtDeclarationWithReturnType extends KtDeclaration {

    /**
     * Returns the type reference for the return type of this declaration, or `null` if the return type is not specified.
     */
    @Nullable
    KtTypeReference getTypeReference();
}
