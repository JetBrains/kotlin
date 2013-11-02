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

package org.jetbrains.jet.asJava;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.compiled.InnerClassSourceStrategy;
import com.intellij.psi.impl.compiled.StubBuildingVisitor;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.jet.codegen.ClassBuilder;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.List;

public class StubClassBuilder extends ClassBuilder {
    private static final InnerClassSourceStrategy<Object> EMPTY_STRATEGY = new InnerClassSourceStrategy<Object>() {
        @Override
        public Object findInnerClass(String s, Object o) {
            return null;
        }

        @Override
        public ClassReader readerForInnerClass(Object o) {
            throw new UnsupportedOperationException("Shall not be called!");
        }
    };
    private final StubElement parent;
    private StubBuildingVisitor v;
    private final Stack<StubElement> parentStack;
    private boolean isNamespace = false;

    public StubClassBuilder(@NotNull Stack<StubElement> parentStack) {
        this.parentStack = parentStack;
        this.parent = parentStack.peek();
    }

    @NotNull
    @Override
    public ClassVisitor getVisitor() {
        assert v != null : "Called before class is defined";
        return v;
    }

    @Override
    public void defineClass(
            PsiElement origin,
            int version,
            int access,
            @NotNull String name,
            @Nullable String signature,
            @NotNull String superName,
            @NotNull String[] interfaces
    ) {
        assert v == null : "defineClass() called twice?";
        v = new StubBuildingVisitor<Object>(null, EMPTY_STRATEGY, parent, access, null);

        super.defineClass(origin, version, access, name, signature, superName, interfaces);

        if (origin instanceof JetFile) {
            FqName packageName = JetPsiUtil.getFQName((JetFile) origin);
            String packageClassName = PackageClassUtils.getPackageClassName(packageName);

            if (name.equals(packageClassName) || name.endsWith("/" + packageClassName)) {
                isNamespace = true;
            }
        }

        if (!isNamespace) {
            parentStack.push(v.getResult());
        }

        ((StubBase) v.getResult()).putUserData(ClsWrapperStubPsiFactory.ORIGIN_ELEMENT, origin);
    }

    @NotNull
    @Override
    public MethodVisitor newMethod(
            @Nullable PsiElement origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable String[] exceptions
    ) {
        MethodVisitor internalVisitor = super.newMethod(origin, access, name, desc, signature, exceptions);

        if (internalVisitor != EMPTY_METHOD_VISITOR) {
            // If stub for method generated
            markLastChild(origin);
        }

        return internalVisitor;
    }

    @NotNull
    @Override
    public FieldVisitor newField(
            @Nullable PsiElement origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable Object value
    ) {
        FieldVisitor internalVisitor = super.newField(origin, access, name, desc, signature, value);

        if (internalVisitor != EMPTY_FIELD_VISITOR) {
            // If stub for field generated
            markLastChild(origin);
        }

        return internalVisitor;
    }

    private void markLastChild(@Nullable PsiElement origin) {
        List children = v.getResult().getChildrenStubs();
        StubBase last = (StubBase) children.get(children.size() - 1);

        PsiElement oldOrigin = last.getUserData(ClsWrapperStubPsiFactory.ORIGIN_ELEMENT);
        if (oldOrigin != null) {
            throw new IllegalStateException("Rewriting origin element: " + oldOrigin.getText() + " for stub " + last.toString());
        }

        last.putUserData(ClsWrapperStubPsiFactory.ORIGIN_ELEMENT, origin);
    }

    @Override
    public void done() {
        if (!isNamespace) {
            StubElement pop = parentStack.pop();
            assert pop == v.getResult();
        }
        super.done();
    }
}
