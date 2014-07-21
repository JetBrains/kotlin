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

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.JvmCodegenUtil;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.CompileTimeConstantUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isClassObject;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumClass;
import static org.jetbrains.jet.lang.types.lang.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class IntrinsicMethods {
    private static final IntrinsicMethod UNARY_MINUS = new UnaryMinus();
    private static final IntrinsicMethod UNARY_PLUS = new UnaryPlus();
    private static final IntrinsicMethod NUMBER_CAST = new NumberCast();
    private static final IntrinsicMethod INV = new Inv();
    private static final IntrinsicMethod RANGE_TO = new RangeTo();
    private static final IntrinsicMethod INC = new Increment(1);
    private static final IntrinsicMethod DEC = new Increment(-1);
    private static final IntrinsicMethod HASH_CODE = new HashCode();

    private static final IntrinsicMethod ARRAY_SIZE = new ArraySize();
    private static final IntrinsicMethod ARRAY_INDICES = new ArrayIndices();
    private static final Equals EQUALS = new Equals();
    private static final IdentityEquals IDENTITY_EQUALS = new IdentityEquals();
    private static final IteratorNext ITERATOR_NEXT = new IteratorNext();
    private static final ArraySet ARRAY_SET = new ArraySet();
    private static final ArrayGet ARRAY_GET = new ArrayGet();
    private static final StringPlus STRING_PLUS = new StringPlus();
    private static final EnumValues ENUM_VALUES = new EnumValues();
    private static final EnumValueOf ENUM_VALUE_OF = new EnumValueOf();
    private static final ToString TO_STRING = new ToString();
    private static final Clone CLONE = new Clone();

    private static final FqNameUnsafe KOTLIN_ANY_FQ_NAME = DescriptorUtils.getFqName(KotlinBuiltIns.getInstance().getAny());
    private static final FqNameUnsafe KOTLIN_STRING_FQ_NAME = DescriptorUtils.getFqName(KotlinBuiltIns.getInstance().getString());

    private final Map<String, IntrinsicMethod> namedMethods = new HashMap<String, IntrinsicMethod>();
    private static final IntrinsicMethod ARRAY_ITERATOR = new ArrayIterator();
    private final IntrinsicsMap intrinsicsMap = new IntrinsicsMap();

    public IntrinsicMethods() {
        namedMethods.put("kotlin.javaClass.function", new JavaClassFunction());
        namedMethods.put("kotlin.javaClass.property", new JavaClassProperty());
        namedMethods.put("kotlin.arrays.array", new JavaClassArray());
        namedMethods.put("kotlin.collections.copyToArray", new CopyToArray());
        namedMethods.put("kotlin.jvm.internal.unsafe.monitorEnter", MonitorInstruction.MONITOR_ENTER);
        namedMethods.put("kotlin.jvm.internal.unsafe.monitorExit", MonitorInstruction.MONITOR_EXIT);

        ImmutableList<Name> primitiveCastMethods = OperatorConventions.NUMBER_CONVERSIONS.asList();
        for (Name method : primitiveCastMethods) {
            String methodName = method.asString();
            declareIntrinsicFunction("Number", methodName, 0, NUMBER_CAST);
            for (PrimitiveType type : PrimitiveType.NUMBER_TYPES) {
                declareIntrinsicFunction(type.getTypeName().asString(), methodName, 0, NUMBER_CAST);
            }
        }

        for (PrimitiveType type : PrimitiveType.NUMBER_TYPES) {
            String typeName = type.getTypeName().asString();
            declareIntrinsicFunction(typeName, "plus", 0, UNARY_PLUS);
            declareIntrinsicFunction(typeName, "minus", 0, UNARY_MINUS);
            declareIntrinsicFunction(typeName, "inv", 0, INV);
            declareIntrinsicFunction(typeName, "rangeTo", 1, RANGE_TO);
            declareIntrinsicFunction(typeName, "inc", 0, INC);
            declareIntrinsicFunction(typeName, "dec", 0, DEC);
        }

        for (PrimitiveType type : PrimitiveType.values()) {
            String typeName = type.getTypeName().asString();
            declareIntrinsicFunction(typeName, "equals", 1, EQUALS);
            declareIntrinsicFunction(typeName, "hashCode", 0, HASH_CODE);
            declareIntrinsicFunction(typeName, "toString", 0, TO_STRING);
        }

        declareBinaryOp("plus", IADD);
        declareBinaryOp("minus", ISUB);
        declareBinaryOp("times", IMUL);
        declareBinaryOp("div", IDIV);
        declareBinaryOp("mod", IREM);
        declareBinaryOp("shl", ISHL);
        declareBinaryOp("shr", ISHR);
        declareBinaryOp("ushr", IUSHR);
        declareBinaryOp("and", IAND);
        declareBinaryOp("or", IOR);
        declareBinaryOp("xor", IXOR);

        declareIntrinsicFunction("Boolean", "not", 0, new Not());

        declareIntrinsicFunction("String", "plus", 1, new Concat());
        declareIntrinsicFunction("CharSequence", "get", 1, new StringGetChar());
        declareIntrinsicFunction("String", "get", 1, new StringGetChar());

        declareIntrinsicFunction("Cloneable", "clone", 0, CLONE);

        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, KOTLIN_ANY_FQ_NAME, "toString", 0, TO_STRING);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, KOTLIN_ANY_FQ_NAME, "equals", 1, EQUALS);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, KOTLIN_ANY_FQ_NAME, "identityEquals", 1, IDENTITY_EQUALS);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, KOTLIN_STRING_FQ_NAME, "plus", 1, STRING_PLUS);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "arrayOfNulls", 1, new NewArray());

        for (PrimitiveType type : PrimitiveType.values()) {
            String typeName = type.getTypeName().asString();
            declareIntrinsicFunction(typeName, "compareTo", 1, new CompareTo());
            declareIntrinsicFunction(typeName + "Iterator", "next", 0, ITERATOR_NEXT);
        }

        declareIntrinsicProperty("CharSequence", "length", new StringLength());
        declareIntrinsicProperty("String", "length", new StringLength());

        declareArrayMethods();
    }

    private void declareArrayMethods() {
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            declareArrayMethodsForPrimitive(jvmPrimitiveType);
        }

        declareIntrinsicProperty("Array", "size", ARRAY_SIZE);
        declareIntrinsicProperty("Array", "indices", ARRAY_INDICES);
        declareIntrinsicFunction("Array", "set", 2, ARRAY_SET);
        declareIntrinsicFunction("Array", "get", 1, ARRAY_GET);
        declareIntrinsicFunction("Array", "clone", 0, CLONE);
        declareIterator("Array");
    }

    private void declareArrayMethodsForPrimitive(@NotNull JvmPrimitiveType jvmPrimitiveType) {
        String arrayTypeName = jvmPrimitiveType.getPrimitiveType().getArrayTypeName().asString();
        declareIntrinsicProperty(arrayTypeName, "size", ARRAY_SIZE);
        declareIntrinsicProperty(arrayTypeName, "indices", ARRAY_INDICES);
        declareIntrinsicFunction(arrayTypeName, "set", 2, ARRAY_SET);
        declareIntrinsicFunction(arrayTypeName, "get", 1, ARRAY_GET);
        declareIntrinsicFunction(arrayTypeName, "clone", 0, CLONE);
        declareIterator(arrayTypeName);
    }

    private void declareIterator(@NotNull String arrayClassName) {
        declareIntrinsicFunction(arrayClassName, "iterator", 0, ARRAY_ITERATOR);
    }

    private void declareBinaryOp(@NotNull String methodName, int opcode) {
        BinaryOp op = new BinaryOp(opcode);
        for (PrimitiveType type : PrimitiveType.values()) {
            declareIntrinsicFunction(type.getTypeName().asString(), methodName, 1, op);
        }
    }

    private void declareIntrinsicProperty(@NotNull String className, @NotNull String methodName, @NotNull IntrinsicMethod implementation) {
        declareIntrinsicFunction(className, methodName, -1, implementation);
    }

    private void declareIntrinsicFunction(
            @NotNull String className,
            @NotNull String methodName,
            int arity,
            @NotNull IntrinsicMethod implementation
    ) {
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME.child(Name.identifier(className)),
                                        null, methodName, arity, implementation);
    }

    @Nullable
    public IntrinsicMethod getIntrinsic(@NotNull CallableMemberDescriptor descriptor) {
        IntrinsicMethod intrinsicMethod = intrinsicsMap.getIntrinsic(descriptor);
        if (intrinsicMethod != null) {
            return intrinsicMethod;
        }

        if (descriptor instanceof SimpleFunctionDescriptor) {
            SimpleFunctionDescriptor functionDescriptor = (SimpleFunctionDescriptor) descriptor;

            DeclarationDescriptor container = descriptor.getContainingDeclaration();
            //noinspection ConstantConditions
            if (isClassObject(container) && isEnumClass(container.getContainingDeclaration())) {
                if (JvmCodegenUtil.isEnumValuesMethod(functionDescriptor)) {
                    return ENUM_VALUES;
                }

                if (JvmCodegenUtil.isEnumValueOfMethod(functionDescriptor)) {
                    return ENUM_VALUE_OF;
                }
            }
        }

        String value = CompileTimeConstantUtils.getIntrinsicAnnotationArgument(descriptor);
        if (value == null) return null;

        return namedMethods.get(value);
    }
}
