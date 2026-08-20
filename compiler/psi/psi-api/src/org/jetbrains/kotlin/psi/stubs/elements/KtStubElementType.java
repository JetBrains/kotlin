/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.util.ArrayFactory;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtElementImplStub;
import org.jetbrains.kotlin.psi.KtImplementationDetail;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

public abstract class KtStubElementType<StubT extends StubElement<?>, PsiT extends KtElementImplStub<?>> extends IStubElementType<StubT, PsiT> {

    @NotNull
    private final Constructor<PsiT> byNodeConstructor;
    @NotNull
    private final Constructor<PsiT> byStubConstructor;
    @NotNull
    private final PsiT[] emptyArray;
    @NotNull
    private final ArrayFactory<PsiT> arrayFactory;

    @SuppressWarnings("unchecked")
    public KtStubElementType(@NotNull @NonNls String debugName, @NotNull Class<PsiT> psiClass, @NotNull Class<?> stubClass) {
        super(debugName, KotlinLanguage.INSTANCE);
        try {
            byNodeConstructor = psiClass.getConstructor(ASTNode.class);
            byStubConstructor = psiClass.getConstructor(stubClass);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("Stub element type declaration for " + psiClass.getSimpleName() + " is missing required constructors",e);
        }
        emptyArray = (PsiT[]) Array.newInstance(psiClass, 0);
        arrayFactory = count -> {
            if (count == 0) {
                return emptyArray;
            }
            return (PsiT[]) Array.newInstance(psiClass, count);
        };
    }

    @NotNull
    public PsiT createPsiFromAst(@NotNull ASTNode node) {
        return ReflectionUtil.createInstance(byNodeConstructor, node);
    }

    @Override
    @NotNull
    public PsiT createPsi(@NotNull StubT stub) {
        return ReflectionUtil.createInstance(byStubConstructor, stub);
    }

    @NotNull
    @Override
    public String getExternalId() {
        return "kotlin." + getDebugName();
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        return createStubDependingOnParent(node);
    }

    @KtImplementationDetail
    public static boolean createStubDependingOnParent(ASTNode node) {
        ASTNode parent = node.getTreeParent();
        IElementType parentType = parent.getElementType();
        if (parentType instanceof IFileElementType) {
            return true;
        }

        StubElementFactory<?, ?> stubFactory = StubElementRegistryService.getInstance().getStubFactory(parentType);
        if (stubFactory != null) {
            return stubFactory.shouldCreateStub(parent);
        }

        return false;
    }

    @Override
    public void indexStub(@NotNull StubT stub, @NotNull IndexSink sink) {
        // do not force inheritors to implement this method
    }

    @NotNull
    public ArrayFactory<PsiT> getArrayFactory() {
        return arrayFactory;
    }
}
