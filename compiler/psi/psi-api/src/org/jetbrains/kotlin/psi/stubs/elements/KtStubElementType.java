/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.stubs.StubSerializer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.ArrayFactory;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtElementImplStub;
import org.jetbrains.kotlin.psi.KtExpression;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

/**
 * Base class for Kotlin stub element types.
 * <p>
 * As part of KT-78356, stub support is being decoupled from element types and moved into dedicated
 * {@link StubElementFactory}/{@link StubSerializer} implementations registered via {@code StubRegistryExtension}.
 * An element type opts into the decoupled API by returning its dedicated implementations from {@link #getStubFactory()}
 * and {@link #getStubSerializer()}; this base then forwards the {@link IStubElementType} contract to them. Element types
 * that have not been migrated yet return {@code null} from the two accessors (the default) and keep overriding the
 * stub methods directly, in which case this base keeps its legacy behavior.
 */
@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public abstract class KtStubElementType<StubT extends StubElement<?>, PsiT extends KtElementImplStub<?>> extends IStubElementType<StubT, PsiT> {

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
            throw new RuntimeException("Stub element type declaration for " + psiClass.getSimpleName() + " is missing required constructors",e);
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
     * The {@link StubElementFactory} that owns this element type's stub creation, or {@code null} for element types that
     * still implement stub creation directly (KT-78356).
     */
    @Nullable
    public StubElementFactory<StubT, PsiT> getStubFactory() {
        return null;
    }

    /**
     * The {@link StubSerializer} that owns this element type's stub serialization, or {@code null} for element types that
     * still implement serialization directly (KT-78356).
     */
    @Nullable
    public StubSerializer<StubT> getStubSerializer() {
        return null;
    }

    @NotNull
    public PsiT createPsiFromAst(@NotNull ASTNode node) {
        return ReflectionUtil.createInstance(byNodeConstructor, node);
    }

    /**
     * Creates the {@link PsiT} for the given {@code stub} via reflection. Exposed so that decoupled stub factories
     * (KT-78356) can build PSI without naming the concrete PSI class (used by the generic ones, e.g. placeholders).
     */
    @NotNull
    public PsiT createPsiFromStub(@NotNull StubT stub) {
        return ReflectionUtil.createInstance(byStubConstructor, stub);
    }

    /**
     * The external id this element type uses by convention ({@code "kotlin." + debug name}). Decoupled stub serializers
     * (KT-78356) reuse it when their id is derived from the debug name rather than being a fixed literal.
     */
    @NotNull
    public final String getConventionalExternalId() {
        return "kotlin." + getDebugName();
    }

    @NotNull
    @Override
    public StubT createStub(@NotNull PsiT psi, StubElement<? extends PsiElement> parentStub) {
        return requireStubFactory().createStub(psi, parentStub);
    }

    @Override
    @NotNull
    public PsiT createPsi(@NotNull StubT stub) {
        StubElementFactory<StubT, PsiT> factory = getStubFactory();
        if (factory != null) {
            PsiT psi = factory.createPsi(stub);
            if (psi != null) {
                return psi;
            }
        }
        return createPsiFromStub(stub);
    }

    @NotNull
    @Override
    public String getExternalId() {
        StubSerializer<StubT> serializer = getStubSerializer();
        return serializer != null ? serializer.getExternalId() : getConventionalExternalId();
    }

    @Override
    public void serialize(@NotNull StubT stub, @NotNull StubOutputStream dataStream) throws IOException {
        requireStubSerializer().serialize(stub, dataStream);
    }

    @NotNull
    @Override
    public StubT deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return requireStubSerializer().deserialize(dataStream, parentStub);
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        StubElementFactory<StubT, PsiT> factory = getStubFactory();
        return factory != null ? factory.shouldCreateStub(node) : shouldCreateStubDependingOnParent(node);
    }

    /**
     * Default {@code shouldCreateStub} policy shared with decoupled stub factories (KT-78356): a node is stubbed only
     * when its parent is stubbed.
     */
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
        StubSerializer<StubT> serializer = getStubSerializer();
        if (serializer != null) {
            serializer.indexStub(stub, sink);
        }
        // else: not indexed by default; legacy element types override this method
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

    @NotNull
    private StubElementFactory<StubT, PsiT> requireStubFactory() {
        StubElementFactory<StubT, PsiT> factory = getStubFactory();
        if (factory == null) {
            throw new IllegalStateException(
                    "Element type " + this + " has no stub factory; legacy element types must override createStub");
        }
        return factory;
    }

    @NotNull
    private StubSerializer<StubT> requireStubSerializer() {
        StubSerializer<StubT> serializer = getStubSerializer();
        if (serializer == null) {
            throw new IllegalStateException(
                    "Element type " + this + " has no stub serializer; legacy element types must override serialize/deserialize");
        }
        return serializer;
    }
}
