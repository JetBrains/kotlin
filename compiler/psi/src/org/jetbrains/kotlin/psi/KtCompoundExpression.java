/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KtCompoundExpression extends KtExpressionImpl{

    public KtCompoundExpression(@NotNull ASTNode node) {super(node);}
    @Nullable
    public List<KtProperty> getProperties() {
        KtProperty[] properties = findChildrenByClass(KtProperty.class);
        return new ArrayList<>(Arrays.asList(properties));
    }
    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitCompoundExpression(this, data);
    }


}