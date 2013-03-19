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
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;

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
    public static final String KOTLIN_JAVA_CLASS_FUNCTION = "kotlin.javaClass.function";
    public static final String KOTLIN_ARRAYS_ARRAY = "kotlin.arrays.array";
    private static final String KOTLIN_JAVA_CLASS_PROPERTY = "kotlin.javaClass.property";
    private static final String KOTLIN_TO_STRING = "kotlin.toString";
    private static final String KOTLIN_HASH_CODE = "kotlin.hashCode";
    private static final EnumValues ENUM_VALUES = new EnumValues();
    private static final EnumValueOf ENUM_VALUE_OF = new EnumValueOf();
    private static final ToString TO_STRING = new ToString();

    private final Map<String, IntrinsicMethod> namedMethods = new HashMap<String, IntrinsicMethod>();
    private static final IntrinsicMethod ARRAY_ITERATOR = new ArrayIterator();
    private final IntrinsicsMap intrinsicsMap = new IntrinsicsMap();


    @PostConstruct
    public void init() {
        namedMethods.put(KOTLIN_JAVA_CLASS_FUNCTION, new JavaClassFunction());
        namedMethods.put(KOTLIN_JAVA_CLASS_PROPERTY, new JavaClassProperty());
        namedMethods.put(KOTLIN_ARRAYS_ARRAY, new JavaClassArray());
        namedMethods.put(KOTLIN_HASH_CODE, HASH_CODE);
        namedMethods.put(KOTLIN_TO_STRING, TO_STRING);

        ImmutableList<Name> primitiveCastMethods = OperatorConventions.NUMBER_CONVERSIONS.asList();
        for (Name method : primitiveCastMethods) {
            declareIntrinsicFunction(Name.identifier("Number"), method, 0, NUMBER_CAST);
            for (PrimitiveType type : PrimitiveType.NUMBER_TYPES) {
                declareIntrinsicFunction(type.getTypeName(), method, 0, NUMBER_CAST);
            }
        }

        for (PrimitiveType type : PrimitiveType.NUMBER_TYPES) {
            Name typeName = type.getTypeName();
            declareIntrinsicFunction(typeName, Name.identifier("plus"), 0, UNARY_PLUS);
            declareIntrinsicFunction(typeName, Name.identifier("minus"), 0, UNARY_MINUS);
            declareIntrinsicFunction(typeName, Name.identifier("inv"), 0, INV);
            declareIntrinsicFunction(typeName, Name.identifier("rangeTo"), 1, RANGE_TO);
            declareIntrinsicFunction(typeName, Name.identifier("inc"), 0, INC);
            declareIntrinsicFunction(typeName, Name.identifier("dec"), 0, DEC);
            declareIntrinsicFunction(typeName, Name.identifier("hashCode"), 0, HASH_CODE);
            declareIntrinsicFunction(typeName, Name.identifier("equals"), 1, EQUALS);
        }

        declareBinaryOp(Name.identifier("plus"), IADD);
        declareBinaryOp(Name.identifier("minus"), ISUB);
        declareBinaryOp(Name.identifier("times"), IMUL);
        declareBinaryOp(Name.identifier("div"), IDIV);
        declareBinaryOp(Name.identifier("mod"), IREM);
        declareBinaryOp(Name.identifier("shl"), ISHL);
        declareBinaryOp(Name.identifier("shr"), ISHR);
        declareBinaryOp(Name.identifier("ushr"), IUSHR);
        declareBinaryOp(Name.identifier("and"), IAND);
        declareBinaryOp(Name.identifier("or"), IOR);
        declareBinaryOp(Name.identifier("xor"), IXOR);

        declareIntrinsicFunction(Name.identifier("Boolean"), Name.identifier("not"), 0, new Not());

        declareIntrinsicFunction(Name.identifier("String"), Name.identifier("plus"), 1, new Concat());
        declareIntrinsicFunction(Name.identifier("CharSequence"), Name.identifier("get"), 1, new StringGetChar());
        declareIntrinsicFunction(Name.identifier("String"), Name.identifier("get"), 1, new StringGetChar());

        FqName builtInsPackageFqName = KotlinBuiltIns.getInstance().getBuiltInsPackageFqName();
        intrinsicsMap.registerIntrinsic(builtInsPackageFqName, Name.identifier("name"), 0, new EnumName());
        intrinsicsMap.registerIntrinsic(builtInsPackageFqName, Name.identifier("ordinal"), 0, new EnumOrdinal());

        intrinsicsMap.registerIntrinsic(builtInsPackageFqName, Name.identifier("toString"), 0, TO_STRING);
        intrinsicsMap.registerIntrinsic(builtInsPackageFqName, Name.identifier("equals"), 1, EQUALS);
        intrinsicsMap.registerIntrinsic(builtInsPackageFqName, Name.identifier("identityEquals"), 1, IDENTITY_EQUALS);
        intrinsicsMap.registerIntrinsic(builtInsPackageFqName, Name.identifier("plus"), 1, STRING_PLUS);
        intrinsicsMap.registerIntrinsic(builtInsPackageFqName, Name.identifier("arrayOfNulls"), 1, new NewArray());
        intrinsicsMap.registerIntrinsic(builtInsPackageFqName, Name.identifier("synchronized"), 2, new StupidSync());
        intrinsicsMap.registerIntrinsic(builtInsPackageFqName, Name.identifier("iterator"), 0, new IteratorIterator());


        declareIntrinsicFunction(Name.identifier("ByteIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("ShortIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("IntIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("LongIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("CharIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("BooleanIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("FloatIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("DoubleIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);

        for (PrimitiveType type : PrimitiveType.values()) {
            declareIntrinsicFunction(type.getTypeName(), Name.identifier("compareTo"), 1, new CompareTo());
        }
        //        declareIntrinsicFunction("Any", "equals", 1, new Equals());
        //
        declareIntrinsicProperty(Name.identifier("CharSequence"), Name.identifier("length"), new StringLength());
        declareIntrinsicProperty(Name.identifier("String"), Name.identifier("length"), new StringLength());

        registerStaticField(getFQName(KotlinBuiltIns.getInstance().getUnit()).toSafe(), Name.identifier("VALUE"));

        for (PrimitiveType type : PrimitiveType.NUMBER_TYPES) {
            registerStaticField(type.getRangeClassName(), Name.identifier("EMPTY"));
        }

        for (PrimitiveType type : PrimitiveType.NUMBER_TYPES) {
            registerRangeOrProgressionProperty(type.getRangeClassName(), Name.identifier("start"));
            registerRangeOrProgressionProperty(type.getRangeClassName(), Name.identifier("end"));

            registerRangeOrProgressionProperty(type.getProgressionClassName(), Name.identifier("start"));
            registerRangeOrProgressionProperty(type.getProgressionClassName(), Name.identifier("end"));
            registerRangeOrProgressionProperty(type.getProgressionClassName(), Name.identifier("increment"));
        }

        declareArrayMethods();
    }

    private void registerStaticField(@NotNull FqName classFqName, @NotNull Name propertyName) {
        FqNameUnsafe classObjectFqName = classFqName.toUnsafe().child(getClassObjectName(classFqName.shortName()));
        intrinsicsMap.registerIntrinsic(classObjectFqName, propertyName, -1, new StaticField(classFqName, propertyName));
    }

    private void registerRangeOrProgressionProperty(@NotNull FqName ownerClass, @NotNull Name propertyName) {
        intrinsicsMap.registerIntrinsic(ownerClass, propertyName, -1, new PropertyOfProgressionOrRange(ownerClass, propertyName));
    }

    private void declareArrayMethods() {

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            declareArrayMethodsForPrimitive(jvmPrimitiveType);
        }

        declareIntrinsicProperty(Name.identifier("Array"), Name.identifier("size"), ARRAY_SIZE);
        declareIntrinsicProperty(Name.identifier("Array"), Name.identifier("indices"), ARRAY_INDICES);
        declareIntrinsicFunction(Name.identifier("Array"), Name.identifier("set"), 2, ARRAY_SET);
        declareIntrinsicFunction(Name.identifier("Array"), Name.identifier("get"), 1, ARRAY_GET);
        declareIterator(Name.identifier("Array"));
    }

    private void declareArrayMethodsForPrimitive(JvmPrimitiveType jvmPrimitiveType) {
        PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
        declareIntrinsicProperty(primitiveType.getArrayTypeName(), Name.identifier("size"), ARRAY_SIZE);
        declareIntrinsicProperty(primitiveType.getArrayTypeName(), Name.identifier("indices"), ARRAY_INDICES);
        declareIntrinsicFunction(primitiveType.getArrayTypeName(), Name.identifier("set"), 2, ARRAY_SET);
        declareIntrinsicFunction(primitiveType.getArrayTypeName(), Name.identifier("get"), 1, ARRAY_GET);
        declareIterator(primitiveType.getArrayTypeName());
    }

    private void declareIterator(@NotNull Name arrayClassName) {
        declareIntrinsicFunction(arrayClassName, Name.identifier("iterator"), 0, ARRAY_ITERATOR);
    }

    private void declareBinaryOp(Name methodName, int opcode) {
        BinaryOp op = new BinaryOp(opcode);
        for (PrimitiveType type : PrimitiveType.values()) {
            declareIntrinsicFunction(type.getTypeName(), methodName, 1, op);
        }
    }

    private void declareIntrinsicProperty(Name className, Name methodName, IntrinsicMethod implementation) {
        intrinsicsMap.registerIntrinsic(KotlinBuiltIns.getInstance().getBuiltInsPackageFqName().child(className), methodName, -1, implementation);
    }

    private void declareIntrinsicFunction(Name className, Name functionName, int arity, IntrinsicMethod implementation) {
        intrinsicsMap.registerIntrinsic(KotlinBuiltIns.getInstance().getBuiltInsPackageFqName().child(className), functionName, arity, implementation);
    }

    @Nullable
    public IntrinsicMethod getIntrinsic(@NotNull CallableMemberDescriptor descriptor) {
        IntrinsicMethod intrinsicMethod = intrinsicsMap.getIntrinsic(descriptor);
        if (intrinsicMethod != null) {
            return intrinsicMethod;
        }

        if (descriptor instanceof SimpleFunctionDescriptor) {
            SimpleFunctionDescriptor functionDescriptor = (SimpleFunctionDescriptor) descriptor;

            if (isEnumClassObject(functionDescriptor.getContainingDeclaration())) {
                if (isEnumValuesMethod(functionDescriptor)) {
                    return ENUM_VALUES;
                }

                if (isEnumValueOfMethod(functionDescriptor)) {
                    return ENUM_VALUE_OF;
                }
            }
        }

        List<AnnotationDescriptor> annotations = descriptor.getAnnotations();
        if (annotations != null) {
            for (AnnotationDescriptor annotation : annotations) {
                ClassifierDescriptor classifierDescriptor = annotation.getType().getConstructor().getDeclarationDescriptor();
                assert classifierDescriptor != null;
                if ("Intrinsic".equals(classifierDescriptor.getName().getName())) {
                    String value = (String) annotation.getAllValueArguments().values().iterator().next().getValue();
                    intrinsicMethod = namedMethods.get(value);
                    if (intrinsicMethod != null) {
                        break;
                    }
                }
            }
        }
        return intrinsicMethod;
    }
}
