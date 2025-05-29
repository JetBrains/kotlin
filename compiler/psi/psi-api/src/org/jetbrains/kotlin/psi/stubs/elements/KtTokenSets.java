/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lang.KotlinOperationPrecedence;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static org.jetbrains.kotlin.KtNodeTypes.*;

public interface KtTokenSets {
    TokenSet DECLARATION_TYPES =
            TokenSet.create(KtNodeTypes.CLASS, OBJECT_DECLARATION, FUN, PROPERTY, TYPEALIAS, CLASS_INITIALIZER, SECONDARY_CONSTRUCTOR, ENUM_ENTRY);

    TokenSet SUPER_TYPE_LIST_ENTRIES = TokenSet.create(DELEGATED_SUPER_TYPE_ENTRY, SUPER_TYPE_CALL_ENTRY, SUPER_TYPE_ENTRY);

    TokenSet TYPE_ELEMENT_TYPES = TokenSet.create(USER_TYPE, NULLABLE_TYPE, FUNCTION_TYPE, DYNAMIC_TYPE, INTERSECTION_TYPE);

    TokenSet INSIDE_DIRECTIVE_EXPRESSIONS = TokenSet.create(REFERENCE_EXPRESSION, DOT_QUALIFIED_EXPRESSION);

    TokenSet CONSTANT_EXPRESSIONS = TokenSet.create(
            NULL,
            BOOLEAN_CONSTANT,
            FLOAT_CONSTANT,
            CHARACTER_CONSTANT,
            INTEGER_CONSTANT,

            REFERENCE_EXPRESSION,
            DOT_QUALIFIED_EXPRESSION,

            STRING_TEMPLATE,

            CLASS_LITERAL_EXPRESSION,

            COLLECTION_LITERAL_EXPRESSION
    );

    TokenSet OPERATIONS = ((Supplier<TokenSet>) () -> {
        Set<IElementType> operations = new HashSet<>();
        for (KotlinOperationPrecedence precedence : KotlinOperationPrecedence.values()) {
            operations.addAll(precedence.getTokens());
        }
        return TokenSet.create(operations.toArray(new IElementType[0]));
    }).get();
}
