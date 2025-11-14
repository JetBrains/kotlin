/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.expressions;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtSingleValueToken;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.utils.OperatorTokens;

import static org.jetbrains.kotlin.util.OperatorNameConventions.*;

/**
 * Should be consistent with {@link org.jetbrains.kotlin.util.OperatorNameConventions}
 */
public class OperatorConventions {

    private OperatorConventions() {}

    // Names for primitive type conversion properties
    public static final Name DOUBLE = TO_DOUBLE;
    public static final Name FLOAT = TO_FLOAT;
    public static final Name LONG = TO_LONG;
    public static final Name INT = TO_INT;
    public static final Name CHAR = TO_CHAR;
    public static final Name SHORT = TO_SHORT;
    public static final Name BYTE = TO_BYTE;


    public static final ImmutableSet<Name> NUMBER_CONVERSIONS = ImmutableSet.of(
            DOUBLE, FLOAT, LONG, INT, SHORT, BYTE, CHAR
    );

    public static final ImmutableBiMap<KtSingleValueToken, Name> UNARY_OPERATION_NAMES =
            ImmutableBiMap.copyOf(OperatorTokens.UNARY_OPERATION_NAMES);

    public static final ImmutableBiMap<KtSingleValueToken, Name> BINARY_OPERATION_NAMES =
            ImmutableBiMap.copyOf(OperatorTokens.BINARY_OPERATION_NAMES);

    public static final ImmutableSet<KtSingleValueToken> NOT_OVERLOADABLE =
            ImmutableSet.of(KtTokens.ANDAND, KtTokens.OROR, KtTokens.ELVIS, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ);
    
    public static final ImmutableSet<KtSingleValueToken> INCREMENT_OPERATIONS =
            ImmutableSet.copyOf(OperatorTokens.INCREMENT_OPERATIONS);

    public static final ImmutableSet<KtSingleValueToken> COMPARISON_OPERATIONS =
            ImmutableSet.copyOf(OperatorTokens.COMPARISON_OPERATIONS);

    public static final ImmutableSet<KtSingleValueToken> EQUALS_OPERATIONS =
            ImmutableSet.copyOf(OperatorTokens.EQUALS_OPERATIONS);

    public static final ImmutableSet<KtSingleValueToken> IDENTITY_EQUALS_OPERATIONS =
            ImmutableSet.copyOf(OperatorTokens.IDENTITY_EQUALS_OPERATIONS);

    public static final ImmutableSet<KtSingleValueToken> IN_OPERATIONS =
            ImmutableSet.copyOf(OperatorTokens.IN_OPERATIONS);

    public static final ImmutableBiMap<KtSingleValueToken, Name> ASSIGNMENT_OPERATIONS =
            ImmutableBiMap.copyOf(OperatorTokens.ASSIGNMENT_OPERATION_NAMES);

    public static final ImmutableBiMap<KtSingleValueToken, KtSingleValueToken> ASSIGNMENT_OPERATION_COUNTERPARTS =
            ImmutableBiMap.copyOf(OperatorTokens.OPERATIONS_FOR_ASSIGNMENTS);

    public static final Name ASSIGN_METHOD = Name.identifier("assign");

    public static final ImmutableBiMap<KtSingleValueToken, Name> BOOLEAN_OPERATIONS = ImmutableBiMap.<KtSingleValueToken, Name>builder()
             .put(KtTokens.ANDAND, AND)
             .put(KtTokens.OROR, OR)
             .build();

    public static final ImmutableSet<Name> CONVENTION_NAMES =
            ImmutableSet.copyOf(OperatorTokens.CONVENTION_NAMES);

    @Nullable
    public static Name getNameForOperationSymbol(@NotNull KtToken token) {
        return OperatorTokens.operationName(token);
    }

    @Nullable
    public static Name getNameForOperationSymbol(@NotNull KtToken token, boolean unaryOperations, boolean binaryOperations) {
        return OperatorTokens.operationName(token, unaryOperations, binaryOperations);
    }

    @Nullable
    public static KtToken getOperationSymbolForName(@NotNull Name name) {
        return OperatorTokens.operationToken(name);
    }

    public static boolean isConventionName(@NotNull Name name) {
        return CONVENTION_NAMES.contains(name) || COMPONENT_REGEX.matches(name.asString());
    }
}
