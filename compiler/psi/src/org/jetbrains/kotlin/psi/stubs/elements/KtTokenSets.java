/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.tree.TokenSet;

import static org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.*;

public interface KtTokenSets {
    TokenSet DECLARATION_TYPES =
            TokenSet.create(CLASS, OBJECT_DECLARATION, FUNCTION, PROPERTY, TYPEALIAS, CLASS_INITIALIZER, SECONDARY_CONSTRUCTOR, ENUM_ENTRY);

    TokenSet SUPER_TYPE_LIST_ENTRIES = TokenSet.create(DELEGATED_SUPER_TYPE_ENTRY, SUPER_TYPE_CALL_ENTRY, SUPER_TYPE_ENTRY);

    TokenSet TYPE_ELEMENT_TYPES = TokenSet.create(USER_TYPE, NULLABLE_TYPE, FUNCTION_TYPE, DYNAMIC_TYPE, INTERSECTION_TYPE);

    TokenSet INSIDE_DIRECTIVE_EXPRESSIONS = TokenSet.create(REFERENCE_EXPRESSION, DOT_QUALIFIED_EXPRESSION);

}
