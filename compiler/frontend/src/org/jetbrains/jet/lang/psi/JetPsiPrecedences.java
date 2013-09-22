/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.parsing.JetExpressionParsing;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.parsing.JetExpressionParsing.Precedence.*;

public class JetPsiPrecedences {
    private static final Logger LOG = Logger.getInstance(JetPsiPrecedences.class);

    private static final Map<IElementType, Integer> precedence;
    static {
        Map<IElementType, Integer> builder = new HashMap<IElementType, Integer>();

        JetExpressionParsing.Precedence[] records = values();
        for (int i = 0; i < records.length; i++) {
            for (IElementType elementType : records[i].getOperations().getTypes()) {
                builder.put(elementType, i);
            }
        }

        precedence = builder;
    }

    public static final int PRECEDENCE_OF_ATOMIC_EXPRESSION = -1;

    public static final int PRECEDENCE_OF_PREFIX_EXPRESSION = PREFIX.ordinal();

    public static final int PRECEDENCE_OF_POSTFIX_EXPRESSION = POSTFIX.ordinal();

    public static int getPrecedence(@NotNull JetExpression expression) {
        if (expression instanceof JetAnnotatedExpression || expression instanceof JetPrefixExpression) {
            return PRECEDENCE_OF_PREFIX_EXPRESSION;
        }
        if (expression instanceof JetPostfixExpression) {
            return PRECEDENCE_OF_POSTFIX_EXPRESSION;
        }
        if (expression instanceof JetOperationExpression) {
            JetOperationExpression operationExpression = (JetOperationExpression) expression;

            IElementType operation = operationExpression.getOperationReference().getReferencedNameElementType();

            Integer precedenceNumber = precedence.get(operation);
            if (precedenceNumber == null) {
                LOG.error("No precedence for operation: " + operation);
                return precedence.size(); // lowest
            }
            return precedenceNumber;
        }
        return PRECEDENCE_OF_ATOMIC_EXPRESSION; // Atomic expression
    }
}
