/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

/*
 * @author max
 */
package org.jetbrains.jet.asJava;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.compiled.InnerClassSourceStrategy;
import com.intellij.psi.impl.compiled.StubBuildingVisitor;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ClassBuilder;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;

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

    public StubClassBuilder(Stack<StubElement> parentStack) {
        this.parentStack = parentStack;
        this.parent = parentStack.peek();
    }

    @Override
    public ClassVisitor getVisitor() {
        assert v != null : "Called before class is defined";
        return v;
    }

    @Override
    public void defineClass(PsiElement origin, int version, int access, String name, @Nullable String signature, String superName, String[] interfaces) {
        assert v == null : "defineClass() called twice?";
        v = new StubBuildingVisitor<Object>(null, EMPTY_STRATEGY, parent, access);

        super.defineClass(origin, version, access, name, signature, superName, interfaces);
        if (name.equals(JvmAbi.PACKAGE_CLASS) || name.endsWith("/" + JvmAbi.PACKAGE_CLASS)) {
            isNamespace = true;
        }
        else {
            parentStack.push(v.getResult());
        }
        
        ((StubBase) v.getResult()).putUserData(ClsWrapperStubPsiFactory.ORIGIN_ELEMENT, origin);
    }

    @Override
    public MethodVisitor newMethod(@Nullable PsiElement origin, int access, String name, String desc, @Nullable String signature, @Nullable String[] exceptions) {
        final MethodVisitor answer = super.newMethod(origin, access, name, desc, signature, exceptions);
        markLastChild(origin);
        return answer;
    }

    @Override
    public FieldVisitor newField(@Nullable PsiElement origin, int access, String name, String desc, @Nullable String signature, @Nullable Object value) {
        final FieldVisitor answer = super.newField(origin, access, name, desc, signature, value);
        markLastChild(origin);
        return answer;
    }

    private void markLastChild(PsiElement origin) {
        final List children = v.getResult().getChildrenStubs();
        StubBase last = (StubBase) children.get(children.size() - 1);
        last.putUserData(ClsWrapperStubPsiFactory.ORIGIN_ELEMENT, origin);
    }

    @Override
    public void done() {
        if (!isNamespace) {
            final StubElement pop = parentStack.pop();
            assert pop == v.getResult();
        }
        super.done();
    }
}
