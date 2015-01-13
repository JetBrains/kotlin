/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.ArrayFactory;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetElementImplStub;
import org.jetbrains.kotlin.psi.JetFunction;
import org.jetbrains.kotlin.psi.JetProperty;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

public abstract class JetStubElementType<StubT extends StubElement, PsiT extends JetElementImplStub<?>> extends IStubElementType<StubT, PsiT> {

    @NotNull
    private final Constructor<PsiT> byNodeConstructor;
    @NotNull
    private final Constructor<PsiT> byStubConstructor;
    @NotNull
    private final PsiT[] emptyArray;
    @NotNull
    private final ArrayFactory<PsiT> arrayFactory;

    public JetStubElementType(@NotNull @NonNls String debugName, @NotNull final Class<PsiT> psiClass, @NotNull Class<?> stubClass) {
        super(debugName, JetLanguage.INSTANCE);
        try {
            byNodeConstructor = psiClass.getConstructor(ASTNode.class);
            byStubConstructor = psiClass.getConstructor(stubClass);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException("Stub element type declaration for " + psiClass.getSimpleName() + " is missing required constructors",e);
        }
        //noinspection unchecked
        emptyArray = (PsiT[]) Array.newInstance(psiClass, 0);
        arrayFactory = new ArrayFactory<PsiT>() {
            @NotNull
            @Override
            public PsiT[] create(int count) {
                if (count == 0) {
                    return emptyArray;
                }
                //noinspection unchecked
                return (PsiT[]) Array.newInstance(psiClass, count);
            }
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
        return "kotlin." + toString();
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        PsiElement psi = node.getPsi();
        if (psi instanceof JetClassOrObject) {
            return true;
        }
        if (psi instanceof JetFunction) {
            return !((JetFunction) psi).isLocal();
        }
        if (psi instanceof JetProperty) {
            return !((JetProperty) psi).isLocal();
        }
        return createStubDependingOnParent(node);
    }

    private static boolean createStubDependingOnParent(ASTNode node) {
        ASTNode parent = node.getTreeParent();
        IElementType parentType = parent.getElementType();
        if (parentType instanceof IStubElementType) {
            return ((IStubElementType) parentType).shouldCreateStub(parent);
        }
        if (parentType instanceof IStubFileElementType) {
            return true;
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
