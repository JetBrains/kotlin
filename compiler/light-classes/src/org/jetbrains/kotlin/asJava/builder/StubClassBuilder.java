/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava.builder;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.compiled.InnerClassSourceStrategy;
import com.intellij.psi.impl.compiled.StubBuildingVisitor;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.AbstractClassBuilder;
import org.jetbrains.kotlin.fileClasses.OldPackageFacadeClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

import java.util.List;

public class StubClassBuilder extends AbstractClassBuilder {
    private static final InnerClassSourceStrategy<Object> EMPTY_STRATEGY = new InnerClassSourceStrategy<Object>() {
        @Override
        public Object findInnerClass(String s, Object o) {
            return null;
        }

        @Override
        public void accept(Object innerClass, StubBuildingVisitor<Object> visitor) {
            throw new UnsupportedOperationException("Shall not be called!");
        }
    };
    private final StubElement parent;
    private final PsiJavaFileStub fileStub;
    private StubBuildingVisitor v;
    private final Stack<StubElement> parentStack;
    private boolean isPackageClass = false;
    private int memberIndex = 0;

    public StubClassBuilder(@NotNull Stack<StubElement> parentStack, @NotNull PsiJavaFileStub fileStub) {
        this.parentStack = parentStack;
        this.parent = parentStack.peek();
        this.fileStub = fileStub;
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

        v = new StubBuildingVisitor<>(null, EMPTY_STRATEGY, parent, access, calculateShortName(name));

        super.defineClass(origin, version, access, name, signature, superName, interfaces);

        if (origin instanceof KtFile) {
            FqName packageName = ((KtFile) origin).getPackageFqName();
            String packageClassName = OldPackageFacadeClassUtils.getPackageClassName(packageName);

            if (name.equals(packageClassName) || name.endsWith("/" + packageClassName)) {
                isPackageClass = true;
            }
        }

        if (!isPackageClass) {
            parentStack.push(v.getResult());
        }

        ((StubBase) v.getResult()).putUserData(ClsWrapperStubPsiFactory.ORIGIN, LightElementOriginKt.toLightClassOrigin(origin));
    }

    @Nullable
    private String calculateShortName(@NotNull String internalName) {
        if (parent instanceof PsiJavaFileStub) {
            assert parent == fileStub;
            String packagePrefix = getPackageInternalNamePrefix();
            assert internalName.startsWith(packagePrefix) : internalName + " : " + packagePrefix;
            return internalName.substring(packagePrefix.length());
        }
        if (parent instanceof PsiClassStub<?>) {
            String parentPrefix = getClassInternalNamePrefix((PsiClassStub) parent);
            if (parentPrefix == null) return null;

            assert internalName.startsWith(parentPrefix) : internalName + " : " + parentPrefix;
            return internalName.substring(parentPrefix.length());
        }
        return null;
    }

    @Nullable
    private String getClassInternalNamePrefix(@NotNull PsiClassStub classStub) {
        String packageName = fileStub.getPackageName();

        String classStubQualifiedName = classStub.getQualifiedName();
        if (classStubQualifiedName == null) return null;

        if (packageName.isEmpty()) {
            return classStubQualifiedName.replace('.', '$') + "$";
        }
        else {
            return packageName.replace('.', '/') + "/" + classStubQualifiedName.substring(packageName.length() + 1).replace('.', '$') + "$";
        }
    }


    @NotNull
    private String getPackageInternalNamePrefix() {
        String packageName = fileStub.getPackageName();
        if (packageName.isEmpty()) {
            return "";
        }
        else {
            return packageName.replace('.', '/') + "/";
        }
    }

    @NotNull
    @Override
    public MethodVisitor newMethod(
            @NotNull JvmDeclarationOrigin origin,
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
            @NotNull JvmDeclarationOrigin origin,
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

    private void markLastChild(@NotNull JvmDeclarationOrigin origin) {
        List children = v.getResult().getChildrenStubs();
        StubBase last = (StubBase) children.get(children.size() - 1);

        LightElementOrigin oldOrigin = last.getUserData(ClsWrapperStubPsiFactory.ORIGIN);
        if (oldOrigin != null) {
            PsiElement originalElement = oldOrigin.getOriginalElement();
            throw new IllegalStateException("Rewriting origin element: " +
                                            (originalElement != null ? originalElement.getText() : null) + " for stub " + last.toString());
        }

        last.putUserData(ClsWrapperStubPsiFactory.ORIGIN, LightElementOriginKt.toLightMemberOrigin(origin));
        last.putUserData(MemberIndex.KEY, new MemberIndex(memberIndex++));
    }

    @Override
    public void done() {
        if (!isPackageClass) {
            StubElement pop = parentStack.pop();
            assert pop == v.getResult() : "parentStack: got " + pop + ", expected " + v.getResult();
        }
        super.done();
    }
}
