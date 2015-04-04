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

package org.jetbrains.kotlin.codegen.intrinsics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.codegen.AsmUtil.asmDescByFqNameWithoutInnerClasses
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.codegen.ExtendedCallable
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.getType

public class ArrayIterator : IntrinsicMethod() {
    override fun generateImpl(codegen: ExpressionCodegen, v: InstructionAdapter, returnType: Type, element: PsiElement?, arguments: List<JetExpression>, receiver: StackValue): Type {
        receiver.put(receiver.type, v)
        val call = element as JetCallExpression
        val funDescriptor = codegen.getBindingContext().get(BindingContext.REFERENCE_TARGET, call.getCalleeExpression() as JetSimpleNameExpression) as FunctionDescriptor
        val containingDeclaration = funDescriptor.getContainingDeclaration().getOriginal() as ClassDescriptor
        if (containingDeclaration == KotlinBuiltIns.getInstance().getArray()) {
            v.invokestatic("kotlin/jvm/internal/InternalPackage", "iterator", "([Ljava/lang/Object;)Ljava/util/Iterator;", false)
            return getType(javaClass<Iterator<Any>>())
        }

        for (jvmPrimitiveType in JvmPrimitiveType.values()) {
            val primitiveType = jvmPrimitiveType.getPrimitiveType()
            val arrayClass = KotlinBuiltIns.getInstance().getPrimitiveArrayClassDescriptor(primitiveType)
            if (containingDeclaration == arrayClass) {
                val fqName = FqName(BUILT_INS_PACKAGE_FQ_NAME.toString() + "." + primitiveType.getTypeName() + "Iterator")
                val iteratorDesc = asmDescByFqNameWithoutInnerClasses(fqName)
                val methodSignature = "([" + jvmPrimitiveType.getDesc() + ")" + iteratorDesc
                v.invokestatic("kotlin/jvm/internal/InternalPackage", "iterator", methodSignature, false)
                return Type.getType(iteratorDesc)
            }
        }

        throw UnsupportedOperationException(containingDeclaration.toString())
    }

    override fun supportCallable(): Boolean {
        return true
    }

    //TODO refactor
    override fun toCallable(state: GenerationState, fd: FunctionDescriptor, context: CodegenContext<*>, isSuper: Boolean): ExtendedCallable {
        val callableMethod = state.getTypeMapper().mapToCallableMethod(fd, false, context)

        val containingDeclaration = fd.getContainingDeclaration().getOriginal() as ClassDescriptor
        var type: Type? = null;
        if (containingDeclaration == KotlinBuiltIns.getInstance().getArray()) {
            type = getType(javaClass<Iterator<Any>>())
        } else {

            for (jvmPrimitiveType in JvmPrimitiveType.values()) {
                val primitiveType = jvmPrimitiveType.getPrimitiveType()
                val arrayClass = KotlinBuiltIns.getInstance().getPrimitiveArrayClassDescriptor(primitiveType)
                if (containingDeclaration == arrayClass) {
                    val fqName = FqName(BUILT_INS_PACKAGE_FQ_NAME.toString() + "." + primitiveType.getTypeName() + "Iterator")
                    val iteratorDesc = asmDescByFqNameWithoutInnerClasses(fqName)
                    type = Type.getType(iteratorDesc)
                    break
                }
            }
        }

        if (type == null) {
            throw UnsupportedOperationException(containingDeclaration.toString())
        }


        return UnaryIntrinsic(callableMethod, type) {
            val containingDeclaration = fd.getContainingDeclaration().getOriginal() as ClassDescriptor
            if (containingDeclaration == KotlinBuiltIns.getInstance().getArray()) {
                it.invokestatic("kotlin/jvm/internal/InternalPackage", "iterator", "([Ljava/lang/Object;)Ljava/util/Iterator;", false)
            } else {
                for (jvmPrimitiveType in JvmPrimitiveType.values()) {
                    val primitiveType = jvmPrimitiveType.getPrimitiveType()
                    val arrayClass = KotlinBuiltIns.getInstance().getPrimitiveArrayClassDescriptor(primitiveType)
                    if (containingDeclaration == arrayClass) {
                        val fqName = FqName(BUILT_INS_PACKAGE_FQ_NAME.toString() + "." + primitiveType.getTypeName() + "Iterator")
                        val iteratorDesc = asmDescByFqNameWithoutInnerClasses(fqName)
                        val methodSignature = "([" + jvmPrimitiveType.getDesc() + ")" + iteratorDesc
                        it.invokestatic("kotlin/jvm/internal/InternalPackage", "iterator", methodSignature, false)
                        break;
                    }
                }
            }
        }
    }
}
