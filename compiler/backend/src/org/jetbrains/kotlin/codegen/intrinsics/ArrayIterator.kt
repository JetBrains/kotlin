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

package org.jetbrains.kotlin.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetCallExpression;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetSimpleNameExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.Iterator;
import java.util.List;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME;
import static org.jetbrains.kotlin.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.getType;

public class ArrayIterator extends IntrinsicMethod {
    @NotNull
    @Override
    public Type generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull Type returnType,
            PsiElement element,
            @NotNull List<JetExpression> arguments,
            @NotNull StackValue receiver
    ) {
        receiver.put(receiver.type, v);
        JetCallExpression call = (JetCallExpression) element;
        FunctionDescriptor funDescriptor = (FunctionDescriptor) codegen.getBindingContext()
                .get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) call.getCalleeExpression());
        assert funDescriptor != null;
        ClassDescriptor containingDeclaration = (ClassDescriptor) funDescriptor.getContainingDeclaration().getOriginal();
        if (containingDeclaration.equals(KotlinBuiltIns.getInstance().getArray())) {
            v.invokestatic("kotlin/jvm/internal/InternalPackage", "iterator", "([Ljava/lang/Object;)Ljava/util/Iterator;", false);
            return getType(Iterator.class);
        }

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            ClassDescriptor arrayClass = KotlinBuiltIns.getInstance().getPrimitiveArrayClassDescriptor(primitiveType);
            if (containingDeclaration.equals(arrayClass)) {
                FqName fqName = new FqName(BUILT_INS_PACKAGE_FQ_NAME + "." + primitiveType.getTypeName() + "Iterator");
                String iteratorDesc = asmDescByFqNameWithoutInnerClasses(fqName);
                String methodSignature = "([" + jvmPrimitiveType.getDesc() + ")" + iteratorDesc;
                v.invokestatic("kotlin/jvm/internal/InternalPackage", "iterator", methodSignature, false);
                return Type.getType(iteratorDesc);
            }
        }

        throw new UnsupportedOperationException(containingDeclaration.toString());
    }
}
