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

package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;

import java.util.Iterator;
import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.getType;
import static org.jetbrains.jet.lang.resolve.java.mapping.PrimitiveTypesUtil.asmTypeForPrimitive;

public class ArrayIterator extends IntrinsicMethod {
    @NotNull
    @Override
    public Type generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull Type returnType,
            PsiElement element,
            List<JetExpression> arguments,
            StackValue receiver
    ) {
        receiver.put(receiver.type, v);
        JetCallExpression call = (JetCallExpression) element;
        FunctionDescriptor funDescriptor = (FunctionDescriptor) codegen.getBindingContext()
                .get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) call.getCalleeExpression());
        assert funDescriptor != null;
        ClassDescriptor containingDeclaration = (ClassDescriptor) funDescriptor.getContainingDeclaration().getOriginal();
        if (containingDeclaration.equals(KotlinBuiltIns.getInstance().getArray())) {
            v.invokestatic("jet/runtime/ArrayIterator", "iterator", "([Ljava/lang/Object;)Ljava/util/Iterator;");
            return getType(Iterator.class);
        }

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            ClassDescriptor arrayClass = KotlinBuiltIns.getInstance().getPrimitiveArrayClassDescriptor(primitiveType);
            if (containingDeclaration.equals(arrayClass)) {
                String iteratorDesc = asmDescByFqNameWithoutInnerClasses(new FqName("jet." + primitiveType.getTypeName() + "Iterator"));
                String methodSignature = "([" + asmTypeForPrimitive(jvmPrimitiveType) + ")" + iteratorDesc;
                v.invokestatic("jet/runtime/ArrayIterator", "iterator", methodSignature);
                return Type.getType(iteratorDesc);
            }
        }

        throw new UnsupportedOperationException(containingDeclaration.toString());
    }
}
