/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.ArrayFactory;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtElementImplStub;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtImplementationDetail;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

/**
 * Base class for Kotlin stub element types.
 * <p>
 * As part of KT-78356, stub support is decoupled from element types: stub creation and serialization live in dedicated
 * {@link StubElementFactory}/{@link StubSerializer} implementations (registered via {@code StubRegistryExtension}), and
 * each element type wires its own through {@link #getStubFactory()} and {@link #getStubSerializer()}. This base simply
 * forwards the {@link IStubElementType} contract to them, so the legacy element-type path and the decoupled registry path
 * share a single implementation.
 */
@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public abstract class KtStubElementType<StubT extends StubElement<?>, PsiT extends KtElementImplStub<?>>
        extends IStubElementType<StubT, PsiT> {

    @NotNull
    private final Constructor<PsiT> byNodeConstructor;
    @NotNull
    private final Constructor<PsiT> byStubConstructor;
    @NotNull
    private final PsiT[] emptyArray;
    @NotNull
    private final ArrayFactory<PsiT> arrayFactory;
    private final boolean isExpression;

    @SuppressWarnings("unchecked")
    public KtStubElementType(@NotNull @NonNls String debugName, @NotNull Class<PsiT> psiClass, @NotNull Class<?> stubClass) {
        super(debugName, KotlinLanguage.INSTANCE);
        try {
            byNodeConstructor = psiClass.getConstructor(ASTNode.class);
            byStubConstructor = psiClass.getConstructor(stubClass);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "Stub element type declaration for " + psiClass.getSimpleName() + " is missing required constructors", e);
        }
        emptyArray = (PsiT[]) Array.newInstance(psiClass, 0);
        arrayFactory = count -> {
            if (count == 0) {
                return emptyArray;
            }
            return (PsiT[]) Array.newInstance(psiClass, count);
        };
        isExpression = KtExpression.class.isAssignableFrom(psiClass);
    }

    /**
     * The {@link StubElementFactory} that owns this element type's stub creation (KT-78356).
     */
    @KtImplementationDetail
    public abstract StubElementFactory<StubT, PsiT> getStubFactory();

    /**
     * The {@link StubSerializer} that owns this element type's stub serialization (KT-78356).
     */
    @KtImplementationDetail
    public abstract StubSerializer<StubT> getStubSerializer();

    @NotNull
    public PsiT createPsiFromAst(@NotNull ASTNode node) {
        return ReflectionUtil.createInstance(byNodeConstructor, node);
    }

    /**
     * Creates the {@link PsiT} for the given {@code stub} via reflection. Used by decoupled stub factories (KT-78356)
     * to build PSI without naming the concrete PSI class (e.g. the generic placeholder factory).
     */
    @KtImplementationDetail
    @NotNull
    public PsiT createPsiFromStub(@NotNull StubT stub) {
        return ReflectionUtil.createInstance(byStubConstructor, stub);
    }

    /**
     * The external id this element type uses by convention ({@code "kotlin." + debug name}). Decoupled stub serializers
     * (KT-78356) reuse it when their id is derived from the debug name rather than being a fixed literal.
     */
    @KtImplementationDetail
    @NotNull
    public final String getConventionalExternalId() {
        return "kotlin." + getDebugName();
    }

    @NotNull
    @Override
    public StubT createStub(@NotNull PsiT psi, StubElement<? extends PsiElement> parentStub) {
        return getStubFactory().createStub(psi, parentStub);
    }

    @Override
    @NotNull
    public PsiT createPsi(@NotNull StubT stub) {
        return createPsiFromStub(stub);
    }

    @NotNull
    @Override
    public String getExternalId() {
        return getStubSerializer().getExternalId();
    }

    @Override
    public void serialize(@NotNull StubT stub, @NotNull StubOutputStream dataStream) throws IOException {
        getStubSerializer().serialize(stub, dataStream);
    }

    @NotNull
    @Override
    public StubT deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return getStubSerializer().deserialize(dataStream, parentStub);
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        return getStubFactory().shouldCreateStub(node);
    }

    /**
     * Default {@code shouldCreateStub} policy shared with decoupled stub factories (KT-78356): a node is stubbed only
     * when its parent is stubbed.
     */
    @KtImplementationDetail
    public static boolean shouldCreateStubDependingOnParent(ASTNode node) {
        ASTNode parent = node.getTreeParent();
        IElementType parentType = parent.getElementType();
        if (parentType instanceof IStubElementType) {
            return ((IStubElementType<?, ?>) parentType).shouldCreateStub(parent);
        }
        if (parentType instanceof IStubFileElementType) {
            return true;
        }
        return false;
    }

    @Override
    public void indexStub(@NotNull StubT stub, @NotNull IndexSink sink) {
        getStubSerializer().indexStub(stub, sink);
    }

    @NotNull
    public ArrayFactory<PsiT> getArrayFactory() {
        return arrayFactory;
    }

    /**
     * @return true when the {@link PsiT} implements {@link KtExpression}.
     */
    public boolean isExpression() {
        return isExpression;
    }
}
