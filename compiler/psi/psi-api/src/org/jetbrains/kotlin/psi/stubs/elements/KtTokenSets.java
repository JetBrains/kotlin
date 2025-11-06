/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtImplementationDetail;

import static org.jetbrains.kotlin.KtNodeTypes.*;

public interface KtTokenSets {
    TokenSet DECLARATION_TYPES =
            TokenSet.create(CLASS, OBJECT_DECLARATION, FUN, PROPERTY, TYPEALIAS, CLASS_INITIALIZER, SECONDARY_CONSTRUCTOR, ENUM_ENTRY);

    TokenSet SUPER_TYPE_LIST_ENTRIES = TokenSet.create(DELEGATED_SUPER_TYPE_ENTRY, SUPER_TYPE_CALL_ENTRY, SUPER_TYPE_ENTRY);

    TokenSet TYPE_ELEMENT_TYPES = TokenSet.create(USER_TYPE, NULLABLE_TYPE, FUNCTION_TYPE, DYNAMIC_TYPE, INTERSECTION_TYPE);

    TokenSet INSIDE_DIRECTIVE_EXPRESSIONS = TokenSet.create(REFERENCE_EXPRESSION, DOT_QUALIFIED_EXPRESSION);

    // typeArguments? valueArguments : typeArguments : arrayAccess
    TokenSet POSTFIX_OPERATIONS = TokenSet.create(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS, KtTokens.EXCLEXCL, KtTokens.DOT, KtTokens.SAFE_ACCESS);

    TokenSet PREFIX_OPERATIONS = TokenSet.create(KtTokens.MINUS, KtTokens.PLUS, KtTokens.MINUSMINUS, KtTokens.PLUSPLUS, KtTokens.EXCL);

    TokenSet CONSTANT_EXPRESSIONS = TokenSet.create(
            NULL,
            BOOLEAN_CONSTANT,
            FLOAT_CONSTANT,
            CHARACTER_CONSTANT,
            INTEGER_CONSTANT,

            REFERENCE_EXPRESSION,
            CALL_EXPRESSION,
            DOT_QUALIFIED_EXPRESSION,

            STRING_TEMPLATE,

            CLASS_LITERAL_EXPRESSION,

            COLLECTION_LITERAL_EXPRESSION
    );
}
