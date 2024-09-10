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

package org.jetbrains.kotlin.codegen.intrinsics;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.builtins.StandardNames;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;
import org.jetbrains.kotlin.util.capitalizeDecapitalize.CapitalizeDecapitalizeKt;
import org.jetbrains.org.objectweb.asm.Type;

import static org.jetbrains.kotlin.builtins.StandardNames.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class IntrinsicMethods {
    public static final String INTRINSICS_CLASS_NAME = "kotlin/jvm/internal/Intrinsics";

    private static final FqName KOTLIN_JVM = new FqName("kotlin.jvm");
    /* package */ static final FqNameUnsafe RECEIVER_PARAMETER_FQ_NAME = new FqNameUnsafe("T");

    private static final FqNameUnsafe KOTLIN_UINT = new FqNameUnsafe("kotlin.UInt");
    private static final FqNameUnsafe KOTLIN_ULONG = new FqNameUnsafe("kotlin.ULong");

    private static final IntrinsicMethod UNARY_MINUS = new UnaryMinus();
    private static final IntrinsicMethod UNARY_PLUS = new UnaryPlus();
    private static final IntrinsicMethod NUMBER_CAST = new NumberCast();
    private static final IntrinsicMethod INV = new Inv();
    private static final IntrinsicMethod RANGE_TO = new RangeTo();
    private static final IntrinsicMethod INC = new Increment(1);
    private static final IntrinsicMethod DEC = new Increment(-1);

    private static final IntrinsicMethod ARRAY_SIZE = new ArraySize();
    private static final IteratorNext ITERATOR_NEXT = new IteratorNext();
    private static final ArraySet ARRAY_SET = new ArraySet();
    private static final ArrayGet ARRAY_GET = new ArrayGet();
    private static final StringPlus STRING_PLUS = new StringPlus();
    private static final ToString TO_STRING = new ToString();
    private static final Clone CLONE = new Clone();

    private static final IntrinsicMethod ARRAY_ITERATOR = new ArrayIterator();
    private final IntrinsicsMap intrinsicsMap = new IntrinsicsMap();

    public IntrinsicMethods(@SuppressWarnings("unused") JvmTarget jvmTarget) {
        this(false);
    }

    public IntrinsicMethods(boolean canReplaceStdlibRuntimeApiBehavior) {
        intrinsicsMap.registerIntrinsic(KOTLIN_JVM, RECEIVER_PARAMETER_FQ_NAME, "javaClass", -1, JavaClassProperty.INSTANCE);
        intrinsicsMap.registerIntrinsic(KOTLIN_JVM, StandardNames.FqNames.kClass, "java", -1, new KClassJavaProperty());
        intrinsicsMap.registerIntrinsic(KOTLIN_JVM, StandardNames.FqNames.kClass, "javaObjectType", -1, new KClassJavaObjectTypeProperty());
        intrinsicsMap.registerIntrinsic(KOTLIN_JVM, StandardNames.FqNames.kClass, "javaPrimitiveType", -1, new KClassJavaPrimitiveTypeProperty());
        intrinsicsMap.registerIntrinsic(StandardNames.FqNames.kCallable.toSafe(), null, "name", -1, new KCallableNameProperty());
        intrinsicsMap.registerIntrinsic(new FqName("kotlin.jvm.internal.unsafe"), null, "monitorEnter", 1, MonitorInstruction.MONITOR_ENTER);
        intrinsicsMap.registerIntrinsic(new FqName("kotlin.jvm.internal.unsafe"), null, "monitorExit", 1, MonitorInstruction.MONITOR_EXIT);
        intrinsicsMap.registerIntrinsic(KOTLIN_JVM, StandardNames.FqNames.array, "isArrayOf", 0, new IsArrayOf());

        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, StandardNames.FqNames.kProperty0, "isInitialized", -1, LateinitIsInitialized.INSTANCE);

        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "arrayOf", 1, new ArrayOf());

        intrinsicsMap.registerIntrinsic(new FqName("kotlin.collections"), new FqNameUnsafe("kotlin.collections.MutableMap"), "set", 2, new MutableMapSet());

        ImmutableList<Name> primitiveCastMethods = OperatorConventions.NUMBER_CONVERSIONS.asList();
        for (Name method : primitiveCastMethods) {
            String methodName = method.asString();
            declareIntrinsicFunction(StandardNames.FqNames.number, methodName, 0, NUMBER_CAST);
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

        IntrinsicMethod hashCode = new HashCode();
        for (PrimitiveType type : PrimitiveType.values()) {
            FqName typeFqName = type.getTypeFqName();
            Type wrapperType = AsmUtil.asmTypeByFqNameWithoutInnerClasses(JvmPrimitiveType.get(type).getWrapperFqName());
            declareIntrinsicFunction(typeFqName, "equals", 1, new EqualsThrowingNpeForNullReceiver(wrapperType));
            declareIntrinsicFunction(typeFqName, "hashCode", 0, hashCode);
            declareIntrinsicFunction(typeFqName, "toString", 0, TO_STRING);

            intrinsicsMap.registerIntrinsic(
                    BUILT_INS_PACKAGE_FQ_NAME, null,
                    CapitalizeDecapitalizeKt.decapitalizeAsciiOnly(type.getArrayTypeName().asString()) + "Of",
                    1, new ArrayOf()
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

        declareIntrinsicFunction(StandardNames.FqNames._boolean, "not", 0, new Not());

        declareIntrinsicFunction(StandardNames.FqNames.string, "plus", 1, new Concat());
        declareIntrinsicFunction(StandardNames.FqNames.string, "get", 1, new StringGetChar());

        if (canReplaceStdlibRuntimeApiBehavior) {
            intrinsicsMap.registerIntrinsic(TEXT_PACKAGE_FQ_NAME, StandardNames.FqNames.string, "trimMargin", 1, new TrimMargin());
            intrinsicsMap.registerIntrinsic(TEXT_PACKAGE_FQ_NAME, StandardNames.FqNames.string, "trimIndent", 0, new TrimIndent());
        }

        declareIntrinsicFunction(StandardNames.FqNames.cloneable, "clone", 0, CLONE);

        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, StandardNames.FqNames.any, "toString", 0, TO_STRING);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, StandardNames.FqNames.string, "plus", 1, STRING_PLUS);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "arrayOfNulls", 1, new NewArray());

        for (PrimitiveType type : PrimitiveType.values()) {
            declareIntrinsicFunction(type.getTypeFqName(), "compareTo", 1, new CompareTo());
            declareIntrinsicFunction(COLLECTIONS_PACKAGE_FQ_NAME.child(Name.identifier(type.getTypeName().asString() + "Iterator")), "next", 0, ITERATOR_NEXT);
        }

        declareArrayMethods();

        Java8UIntDivide java8UIntDivide = new Java8UIntDivide();
        intrinsicsMap.registerIntrinsic(KOTLIN_UINT.toSafe(), null, "div", 1, java8UIntDivide);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "uintDivide", 2, java8UIntDivide);

        Java8UIntRemainder java8UIntRemainder = new Java8UIntRemainder();
        intrinsicsMap.registerIntrinsic(KOTLIN_UINT.toSafe(), null, "rem", 1, java8UIntRemainder);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "uintRemainder", 2, java8UIntRemainder);

        Java8UIntCompare java8UIntCompare = new Java8UIntCompare();
        intrinsicsMap.registerIntrinsic(KOTLIN_UINT.toSafe(), null, "compareTo", 1, java8UIntCompare);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "uintCompare", 2, java8UIntCompare);

        intrinsicsMap.registerIntrinsic(KOTLIN_UINT.toSafe(), null, "toString", 0, new Java8UIntToString());

        Java8ULongDivide java8ULongDivide = new Java8ULongDivide();
        intrinsicsMap.registerIntrinsic(KOTLIN_ULONG.toSafe(), null, "div", 1, java8ULongDivide);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "ulongDivide", 2, java8ULongDivide);

        Java8ULongRemainder java8ULongRemainder = new Java8ULongRemainder();
        intrinsicsMap.registerIntrinsic(KOTLIN_ULONG.toSafe(), null, "rem", 1, java8ULongRemainder);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "ulongRemainder", 2, java8ULongRemainder);

        Java8ULongCompare java8ULongCompare = new Java8ULongCompare();
        intrinsicsMap.registerIntrinsic(KOTLIN_ULONG.toSafe(), null, "compareTo", 1, java8ULongCompare);
        intrinsicsMap.registerIntrinsic(BUILT_INS_PACKAGE_FQ_NAME, null, "ulongCompare", 2, java8ULongCompare);

        intrinsicsMap.registerIntrinsic(KOTLIN_ULONG.toSafe(), null, "toString", 0, new Java8ULongToString());
    }

    private void declareArrayMethods() {
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            declareArrayMethods(jvmPrimitiveType.getPrimitiveType().getArrayTypeFqName());
        }
        declareArrayMethods(FqNames.array.toSafe());
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
