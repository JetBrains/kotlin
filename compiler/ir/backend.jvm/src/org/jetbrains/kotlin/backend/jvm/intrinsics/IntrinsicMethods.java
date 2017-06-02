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

package org.jetbrains.kotlin.backend.jvm.intrinsics;

import com.google.common.collect.ImmutableList;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class IntrinsicMethods {
    public static final String INTRINSICS_CLASS_NAME = "kotlin/jvm/internal/Intrinsics";

    private static final FqName KOTLIN_JVM = new FqName("kotlin.jvm");
    /* package */ static final FqNameUnsafe RECEIVER_PARAMETER_FQ_NAME = new FqNameUnsafe("T");

    private static final IntrinsicMethod UNARY_MINUS = new UnaryMinus();
    private static final IntrinsicMethod UNARY_PLUS = new UnaryPlus();
    private static final IntrinsicMethod NUMBER_CAST = new NumberCast();
    private static final IntrinsicMethod INV = new Inv();
    private static final IntrinsicMethod RANGE_TO = new RangeTo();
    private static final IntrinsicMethod INC = new Increment(1);
    private static final IntrinsicMethod DEC = new Increment(-1);
    private static final IntrinsicMethod HASH_CODE = new HashCode();

    private static final IntrinsicMethod ARRAY_SIZE = new ArraySize();
    private static final Equals EQUALS = new Equals(KtTokens.EQEQ);
    private static final IteratorNext ITERATOR_NEXT = new IteratorNext();
    private static final ArraySet ARRAY_SET = new ArraySet();
    private static final ArrayGet ARRAY_GET = new ArrayGet();
    private static final StringPlus STRING_PLUS = new StringPlus();
    private static final ToString TO_STRING = new ToString();
    private static final Clone CLONE = new Clone();

    private static final IntrinsicMethod ARRAY_ITERATOR = new ArrayIterator();
    private final IntrinsicsMap intrinsicsMap = new IntrinsicsMap();

    public IntrinsicMethods() {
        intrinsicsMap.registerIntrinsic(KOTLIN_JVM, RECEIVER_PARAMETER_FQ_NAME, "javaClass", -1, JavaClassProperty.INSTANCE);
        intrinsicsMap.registerIntrinsic(KOTLIN_JVM, KotlinBuiltIns.FQ_NAMES.kClass, "java", -1, new KClassJavaProperty());
        intrinsicsMap.registerIntrinsic(new FqName("kotlin.jvm.internal.unsafe"), null, "monitorEnter", 1, MonitorInstruction.MONITOR_ENTER);
        intrinsicsMap.registerIntrinsic(new FqName("kotlin.jvm.internal.unsafe"), null, "monitorExit", 1, MonitorInstruction.MONITOR_EXIT);
        intrinsicsMap.registerIntrinsic(KOTLIN_JVM, KotlinBuiltIns.FQ_NAMES.array, "isArrayOf", 0, new IsArrayOf());

        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "arrayOf", 1, new ArrayOf());

        ImmutableList<Name> primitiveCastMethods = OperatorConventions.NUMBER_CONVERSIONS.asList();
        for (Name method : primitiveCastMethods) {
            String methodName = method.asString();
            declareIntrinsicFunction(FQ_NAMES.number, methodName, 0, NUMBER_CAST);
            for (PrimitiveType type : PrimitiveType.NUMBER_TYPES) {
                declareIntrinsicFunction(type.getTypeFqName(), methodName, 0, NUMBER_CAST);
            }
        }

        for (PrimitiveType type : PrimitiveType.NUMBER_TYPES) {
            FqName typeFqName = type.getTypeFqName();
            declareIntrinsicFunction(typeFqName, "plus", 0, UNARY_PLUS);
            declareIntrinsicFunction(typeFqName, "unaryPlus", 0, UNARY_PLUS);
            declareIntrinsicFunction(typeFqName, "minus", 0, UNARY_MINUS);
            declareIntrinsicFunction(typeFqName, "unaryMinus", 0, UNARY_MINUS);
            declareIntrinsicFunction(typeFqName, "inv", 0, INV);
            declareIntrinsicFunction(typeFqName, "rangeTo", 1, RANGE_TO);
            declareIntrinsicFunction(typeFqName, "inc", 0, INC);
            declareIntrinsicFunction(typeFqName, "dec", 0, DEC);
        }

        for (PrimitiveType type : PrimitiveType.values()) {
            FqName typeFqName = type.getTypeFqName();
            declareIntrinsicFunction(typeFqName, "equals", 1, EQUALS);
            declareIntrinsicFunction(typeFqName, "hashCode", 0, HASH_CODE);
            declareIntrinsicFunction(typeFqName, "toString", 0, TO_STRING);

            intrinsicsMap.registerIntrinsic(
                    BUILT_INS_PACKAGE_FQ_NAME, null, StringsKt.decapitalize(type.getArrayTypeName().asString()) + "Of", 1, new ArrayOf()
            );
        }

        declareBinaryOp("plus", IADD);
        declareBinaryOp("minus", ISUB);
        declareBinaryOp("times", IMUL);
        declareBinaryOp("div", IDIV);
        declareBinaryOp("mod", IREM);
        declareBinaryOp("rem", IREM);
        declareBinaryOp("shl", ISHL);
        declareBinaryOp("shr", ISHR);
        declareBinaryOp("ushr", IUSHR);
        declareBinaryOp("and", IAND);
        declareBinaryOp("or", IOR);
        declareBinaryOp("xor", IXOR);

        declareIntrinsicFunction(FQ_NAMES._boolean, "not", 0, new Not());

        declareIntrinsicFunction(FQ_NAMES.string, "plus", 1, new Concat());
        declareIntrinsicFunction(FQ_NAMES.string, "get", 1, new StringGetChar());

        declareIntrinsicFunction(FQ_NAMES.cloneable, "clone", 0, CLONE);

        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, KotlinBuiltIns.FQ_NAMES.any, "toString", 0, TO_STRING);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, KotlinBuiltIns.FQ_NAMES.string, "plus", 1, STRING_PLUS);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "arrayOfNulls", 1, new NewArray());

        for (PrimitiveType type : PrimitiveType.values()) {
            declareIntrinsicFunction(type.getTypeFqName(), "compareTo", 1, new CompareTo());
            declareIntrinsicFunction(COLLECTIONS_PACKAGE_FQ_NAME.child(Name.identifier(type.getTypeName().asString() + "Iterator")), "next", 0, ITERATOR_NEXT);
        }

        declareArrayMethods();
    }

    private void declareArrayMethods() {
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            declareArrayMethods(jvmPrimitiveType.getPrimitiveType().getArrayTypeFqName());
        }
        declareArrayMethods(FQ_NAMES.array.toSafe());
    }

    private void declareArrayMethods(@NotNull FqName arrayTypeFqName) {
        declareIntrinsicFunction(arrayTypeFqName, "size", -1, ARRAY_SIZE);
        declareIntrinsicFunction(arrayTypeFqName, "set", 2, ARRAY_SET);
        declareIntrinsicFunction(arrayTypeFqName, "get", 1, ARRAY_GET);
        declareIntrinsicFunction(arrayTypeFqName, "clone", 0, CLONE);
        declareIntrinsicFunction(arrayTypeFqName, "iterator", 0, ARRAY_ITERATOR);
        declareIntrinsicFunction(arrayTypeFqName, "<init>", 2, ArrayConstructor.INSTANCE);
    }

    private void declareBinaryOp(@NotNull String methodName, int opcode) {
        BinaryOp op = new BinaryOp(opcode);
        for (PrimitiveType type : PrimitiveType.values()) {
            declareIntrinsicFunction(type.getTypeFqName(), methodName, 1, op);
        }
    }

    private void declareIntrinsicFunction(
            @NotNull FqName classFqName,
            @NotNull String methodName,
            int arity,
            @NotNull IntrinsicMethod implementation
    ) {
        intrinsicsMap.registerIntrinsic(classFqName, null, methodName, arity, implementation);
    }

    private void declareIntrinsicFunction(
            @NotNull FqNameUnsafe classFqName,
            @NotNull String methodName,
            int arity,
            @NotNull IntrinsicMethod implementation
    ) {
        intrinsicsMap.registerIntrinsic(classFqName.toSafe(), null, methodName, arity, implementation);
    }

    @Nullable
    public IntrinsicMethod getIntrinsic(@NotNull CallableMemberDescriptor descriptor) {
        return intrinsicsMap.getIntrinsic(descriptor);
    }
}
