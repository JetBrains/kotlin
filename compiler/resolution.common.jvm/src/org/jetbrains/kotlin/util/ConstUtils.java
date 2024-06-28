/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util;

import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsFieldImpl;
import com.intellij.psi.impl.java.stubs.PsiFieldStub;
import com.intellij.psi.impl.source.PsiFieldImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.impl.NotEvaluatedConstAware;

import java.util.HashMap;
import java.util.Map;

public class ConstUtils {
    // Copy paste from com.intellij.psi.util.PsiUtil.isCompileTimeConstant
    public static boolean isCompileTimeConstant(@NotNull PsiVariable field) {
        if (!field.hasModifierProperty(PsiModifier.FINAL)) return false;
        PsiType type = field.getType();
        return (TypeConversionUtil.isPrimitiveAndNotNull(type) || type.equalsToText(CommonClassNames.JAVA_LANG_STRING))
               && field.hasInitializer()
               && isConstantExpression(field.getInitializer());
    }

    // Copy paste from com.intellij.psi.util.PsiUtil.isConstantExpression
    private static boolean isConstantExpression(@Nullable PsiExpression expression) {
        if (expression == null) return false;
        IsConstantExpressionVisitor visitor = new IsConstantExpressionVisitor();
        expression.accept(visitor);
        return visitor.isConstant();
    }
}

// Copy of `com.intellij.psi.util.IsConstantExpressionVisitor`.
// This copy is required to be able to handle K2 constants without triggering constant evaluation.
// There are two major changes:
// 1. In `visitReferenceExpression` where we check for constant expression without triggering evaluation.
// 2. In `visitPolyadicExpression` before we check for the type.
// At the moment when we check for constant, not all types could be resolved, and if we return a wrong type, it will be cached.
final class IsConstantExpressionVisitor extends JavaElementVisitor {
    private boolean myIsConstant;
    private final Map<PsiVariable, Boolean> varIsConst = new HashMap<>();

    public boolean isConstant() {
        return myIsConstant;
    }

    @Override
    public void visitExpression(PsiExpression expression) {
        myIsConstant = false;
    }

    @Override
    public void visitLiteralExpression(PsiLiteralExpression expression) {
        myIsConstant = !"null".equals(expression.getText());
    }

    @Override
    public void visitClassObjectAccessExpression(PsiClassObjectAccessExpression expression) {
        myIsConstant = true;
    }

    @Override
    public void visitParenthesizedExpression(PsiParenthesizedExpression expression) {
        PsiExpression expr = expression.getExpression();
        if (expr != null) {
            expr.accept(this);
        }
    }

    @Override
    public void visitTypeCastExpression(PsiTypeCastExpression expression) {
        PsiExpression operand = expression.getOperand();
        if (operand == null) {
            myIsConstant = false;
            return;
        }
        operand.accept(this);
        if (!myIsConstant) return;
        PsiTypeElement element = expression.getCastType();
        if (element == null) {
            myIsConstant = false;
            return;
        }

        PsiType type = element.getType();
        if (type instanceof PsiPrimitiveType) return;
        if (type.equalsToText(CommonClassNames.JAVA_LANG_STRING)) return;
        myIsConstant = false;
    }

    @Override
    public void visitPrefixExpression(PsiPrefixExpression expression) {
        PsiExpression operand = expression.getOperand();
        if (operand == null) {
            myIsConstant = false;
            return;
        }
        operand.accept(this);
        if (!myIsConstant) return;
        IElementType opType = expression.getOperationTokenType();
        if (opType == JavaTokenType.PLUS || opType == JavaTokenType.MINUS || opType == JavaTokenType.TILDE || opType == JavaTokenType.EXCL) {
            return;
        }
        myIsConstant = false;
    }

    @Override
    public void visitPolyadicExpression(PsiPolyadicExpression expression) {
        for (PsiExpression operand : expression.getOperands()) {
            operand.accept(this);
            if (!myIsConstant) return;

            // CHANGE #1
            if (checkForNotYetEvaluatedConstant(operand)) return;
            // END

            final PsiType type = operand.getType();
            if (type != null && !(type instanceof PsiPrimitiveType) && !type.equalsToText(CommonClassNames.JAVA_LANG_STRING)) {
                myIsConstant = false;
                return;
            }
        }
    }

    @Override
    public void visitConditionalExpression(PsiConditionalExpression expression) {
        PsiExpression thenExpr = expression.getThenExpression();
        PsiExpression elseExpr = expression.getElseExpression();
        if (thenExpr == null || elseExpr == null) {
            myIsConstant = false;
            return;
        }

        expression.getCondition().accept(this);
        if (!myIsConstant) return;
        thenExpr.accept(this);
        if (!myIsConstant) return;
        elseExpr.accept(this);
    }

    @Override
    public void visitReferenceExpression(PsiReferenceExpression expression) {
        PsiExpression qualifierExpression = expression.getQualifierExpression();
        if (qualifierExpression != null && !(qualifierExpression instanceof PsiReferenceExpression)) {
            myIsConstant = false;
            return;
        }
        PsiElement refElement = expression.resolve();
        if (!(refElement instanceof PsiVariable)) {
            myIsConstant = false;
            return;
        }
        PsiVariable variable = (PsiVariable)refElement;
        Boolean isConst = varIsConst.get(variable);
        if (isConst != null) {
            myIsConstant &= isConst.booleanValue();
            return;
        }
        if (variable instanceof PsiEnumConstant) {
            myIsConstant = true;
            varIsConst.put(variable, Boolean.TRUE);
            return;
        }
        varIsConst.put(variable, Boolean.FALSE);
        if (!variable.hasModifierProperty(PsiModifier.FINAL)){
            myIsConstant = false;
            return;
        }

        // CHANGE #2
        if (checkForNotYetEvaluatedConstant(expression)) return;
        // END

        variable.hasInitializer();
        PsiExpression initializer = PsiFieldImpl.getDetachedInitializer(variable);
        if (initializer == null){
            myIsConstant = false;
            return;
        }
        initializer.accept(this);
        varIsConst.put(variable, myIsConstant);
    }

    private boolean checkForNotYetEvaluatedConstant(PsiExpression operand) {
        if (operand instanceof PsiReferenceExpression) {
            PsiElement refElement = ((PsiReferenceExpression) operand).resolve();
            NotEvaluatedConstAware notEvaluatedConstAware = getNotEvaluatedConstAware(refElement);
            if (notEvaluatedConstAware != null) {
                if (notEvaluatedConstAware.isNotYetComputed()) {
                    myIsConstant = true;
                    varIsConst.put((PsiVariable) refElement, Boolean.TRUE);
                }
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static NotEvaluatedConstAware getNotEvaluatedConstAware(PsiElement refElement) {
        if (refElement instanceof ClsFieldImpl) {
            PsiFieldStub stub = ((ClsFieldImpl) refElement).getStub();
            if (stub instanceof NotEvaluatedConstAware) {
                return (NotEvaluatedConstAware) stub;
            }
        } else if (refElement instanceof NotEvaluatedConstAware) {
            return (NotEvaluatedConstAware) refElement;
        }
        return null;
    }
}
