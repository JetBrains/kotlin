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
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.Iterator;
import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.getType;
import static org.jetbrains.jet.lang.resolve.java.mapping.PrimitiveTypesUtil.asmTypeForPrimitive;
import static org.jetbrains.jet.lang.types.lang.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME;

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
            v.invokestatic("kotlin/jvm/internal/InternalPackage", "iterator", "([Ljava/lang/Object;)Ljava/util/Iterator;", false);
            return getType(Iterator.class);
        }

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            ClassDescriptor arrayClass = KotlinBuiltIns.getInstance().getPrimitiveArrayClassDescriptor(primitiveType);
            if (containingDeclaration.equals(arrayClass)) {
                FqName fqName = new FqName(BUILT_INS_PACKAGE_FQ_NAME + "." + primitiveType.getTypeName() + "Iterator");
                String iteratorDesc = asmDescByFqNameWithoutInnerClasses(fqName);
                String methodSignature = "([" + asmTypeForPrimitive(jvmPrimitiveType) + ")" + iteratorDesc;
                v.invokestatic("kotlin/jvm/internal/InternalPackage", "iterator", methodSignature, false);
                return Type.getType(iteratorDesc);
            }
        }

        throw new UnsupportedOperationException(containingDeclaration.toString());
    }
}
