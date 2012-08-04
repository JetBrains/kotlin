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

package org.jetbrains.jet.codegen.intrinsics;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.asm4.Opcodes;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;

/**
 * @author yole
 * @author alex.tkachman
 */
public class IntrinsicMethods {
    private static final IntrinsicMethod UNARY_MINUS = new UnaryMinus();
    private static final IntrinsicMethod UNARY_PLUS = new UnaryPlus();
    private static final IntrinsicMethod NUMBER_CAST = new NumberCast();
    private static final IntrinsicMethod INV = new Inv();
    private static final IntrinsicMethod UP_TO = new UpTo(true);
    private static final IntrinsicMethod DOWN_TO = new UpTo(false);
    private static final IntrinsicMethod INC = new Increment(1);
    private static final IntrinsicMethod DEC = new Increment(-1);
    private static final IntrinsicMethod HASH_CODE = new HashCode();

    private static final List<Name> PRIMITIVE_TYPES = ImmutableList.of(Name.identifier("Boolean"), Name.identifier("Byte"), Name.identifier("Char"), Name.identifier("Short"), Name.identifier("Int"), Name.identifier("Float"), Name.identifier("Long"), Name.identifier("Double"));
    private static final List<Name> PRIMITIVE_NUMBER_TYPES = ImmutableList.of(Name.identifier("Byte"), Name.identifier("Char"), Name.identifier("Short"), Name.identifier("Int"), Name.identifier("Float"), Name.identifier("Long"), Name.identifier("Double"));
    public static final IntrinsicMethod ARRAY_SIZE = new ArraySize();
    public static final IntrinsicMethod ARRAY_INDICES = new ArrayIndices();
    public static final Equals EQUALS = new Equals();
    public static final IdentityEquals IDENTITY_EQUALS = new IdentityEquals();
    public static final IteratorNext ITERATOR_NEXT = new IteratorNext();
    public static final ArraySet ARRAY_SET = new ArraySet();
    public static final ArrayGet ARRAY_GET = new ArrayGet();
    public static final StringPlus STRING_PLUS = new StringPlus();
    public static final String KOTLIN_JAVA_CLASS_FUNCTION = "kotlin.javaClass.function";
    public static final String KOTLIN_ARRAYS_ARRAY = "kotlin.arrays.array";
    public static final String KOTLIN_JAVA_CLASS_PROPERTY = "kotlin.javaClass.property";

    private Project myProject;
    private final Map<String, IntrinsicMethod> namedMethods = new HashMap<String, IntrinsicMethod>();
    private static final IntrinsicMethod ARRAY_ITERATOR = new ArrayIterator();
    private final IntrinsicsMap intrinsicsMap = new IntrinsicsMap();


    @Inject
    public void setMyProject(Project myProject) {
        this.myProject = myProject;
    }

    @PostConstruct
    public void init() {
        namedMethods.put(KOTLIN_JAVA_CLASS_FUNCTION, new JavaClassFunction());
        namedMethods.put(KOTLIN_JAVA_CLASS_PROPERTY, new JavaClassProperty());
        namedMethods.put(KOTLIN_ARRAYS_ARRAY, new JavaClassArray());

        ImmutableList<Name> primitiveCastMethods = OperatorConventions.NUMBER_CONVERSIONS.asList();
        for (Name method : primitiveCastMethods) {
            declareIntrinsicFunction(Name.identifier("Number"), method, 0, NUMBER_CAST);
            for (Name type : PRIMITIVE_NUMBER_TYPES) {
                declareIntrinsicFunction(type, method, 0, NUMBER_CAST);
            }
        }

        for (Name type : PRIMITIVE_NUMBER_TYPES) {
            declareIntrinsicFunction(type, Name.identifier("plus"), 0, UNARY_PLUS);
            declareIntrinsicFunction(type, Name.identifier("minus"), 0, UNARY_MINUS);
            declareIntrinsicFunction(type, Name.identifier("inv"), 0, INV);
            declareIntrinsicFunction(type, Name.identifier("rangeTo"), 1, UP_TO);
            declareIntrinsicFunction(type, Name.identifier("upto"), 1, UP_TO);
            declareIntrinsicFunction(type, Name.identifier("downto"), 1, DOWN_TO);
            declareIntrinsicFunction(type, Name.identifier("inc"), 0, INC);
            declareIntrinsicFunction(type, Name.identifier("dec"), 0, DEC);
            declareIntrinsicFunction(type, Name.identifier("hashCode"), 0, HASH_CODE);
            declareIntrinsicFunction(type, Name.identifier("equals"), 1, EQUALS);
        }

        declareBinaryOp(Name.identifier("plus"), Opcodes.IADD);
        declareBinaryOp(Name.identifier("minus"), Opcodes.ISUB);
        declareBinaryOp(Name.identifier("times"), Opcodes.IMUL);
        declareBinaryOp(Name.identifier("div"), Opcodes.IDIV);
        declareBinaryOp(Name.identifier("mod"), Opcodes.IREM);
        declareBinaryOp(Name.identifier("shl"), Opcodes.ISHL);
        declareBinaryOp(Name.identifier("shr"), Opcodes.ISHR);
        declareBinaryOp(Name.identifier("ushr"), Opcodes.IUSHR);
        declareBinaryOp(Name.identifier("and"), Opcodes.IAND);
        declareBinaryOp(Name.identifier("or"), Opcodes.IOR);
        declareBinaryOp(Name.identifier("xor"), Opcodes.IXOR);

        declareIntrinsicFunction(Name.identifier("Boolean"), Name.identifier("not"), 0, new Not());

        declareIntrinsicFunction(Name.identifier("String"), Name.identifier("plus"), 1, new Concat());
        declareIntrinsicFunction(Name.identifier("CharSequence"), Name.identifier("get"), 1, new StringGetChar());
        declareIntrinsicFunction(Name.identifier("String"), Name.identifier("get"), 1, new StringGetChar());

        intrinsicsMap.registerIntrinsic(JetStandardClasses.STANDARD_CLASSES_FQNAME, Name.identifier("toString"), 0, new ToString());
        intrinsicsMap.registerIntrinsic(JetStandardClasses.STANDARD_CLASSES_FQNAME, Name.identifier("equals"), 1, EQUALS);
        intrinsicsMap.registerIntrinsic(JetStandardClasses.STANDARD_CLASSES_FQNAME, Name.identifier("identityEquals"), 1, IDENTITY_EQUALS);
        intrinsicsMap.registerIntrinsic(JetStandardClasses.STANDARD_CLASSES_FQNAME, Name.identifier("plus"), 1, STRING_PLUS);
        intrinsicsMap.registerIntrinsic(JetStandardClasses.STANDARD_CLASSES_FQNAME, Name.identifier("arrayOfNulls"), 1, new NewArray());
        intrinsicsMap.registerIntrinsic(JetStandardClasses.STANDARD_CLASSES_FQNAME, Name.identifier("sure"), 0, new Sure());
        intrinsicsMap.registerIntrinsic(JetStandardClasses.STANDARD_CLASSES_FQNAME, Name.identifier("synchronized"), 2, new StupidSync());
        intrinsicsMap.registerIntrinsic(JetStandardClasses.STANDARD_CLASSES_FQNAME, Name.identifier("iterator"), 0, new IteratorIterator());


        declareIntrinsicFunction(Name.identifier("ByteIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("ShortIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("IntIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("LongIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("CharIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("BooleanIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("FloatIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);
        declareIntrinsicFunction(Name.identifier("DoubleIterator"), Name.identifier("next"), 0, ITERATOR_NEXT);

        for (Name type : PRIMITIVE_TYPES) {
            declareIntrinsicFunction(type, Name.identifier("compareTo"), 1, new CompareTo());
        }
//        declareIntrinsicFunction("Any", "equals", 1, new Equals());
//
        declareIntrinsicProperty(Name.identifier("CharSequence"), Name.identifier("length"), new StringLength());
        declareIntrinsicProperty(Name.identifier("String"), Name.identifier("length"), new StringLength());

        declareArrayMethods();
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
        for (Name type : PRIMITIVE_TYPES) {
            declareIntrinsicFunction(type, methodName, 1, op);
        }
    }

    private void declareIntrinsicProperty(Name className, Name methodName, IntrinsicMethod implementation) {
        intrinsicsMap.registerIntrinsic(JetStandardClasses.STANDARD_CLASSES_FQNAME.child(className), methodName, -1, implementation);
    }

    private void declareIntrinsicFunction(Name className, Name functionName, int arity, IntrinsicMethod implementation) {
        intrinsicsMap.registerIntrinsic(JetStandardClasses.STANDARD_CLASSES_FQNAME.child(className), functionName, arity, implementation);
    }

    public IntrinsicMethod isIntrinsicMethod(DeclarationDescriptor descriptor) {
        List<AnnotationDescriptor> annotations = descriptor.getAnnotations();
        if (annotations != null) {
            for (AnnotationDescriptor annotation : annotations) {
                if("Intrinsic".equals(annotation.getType().getConstructor().getDeclarationDescriptor().getName().getName())) {
                    String value = (String) annotation.getValueArguments().get(0).getValue();
                    IntrinsicMethod intrinsicMethod = namedMethods.get(value);
                    if (intrinsicMethod != null) { return intrinsicMethod; }
                }
            }
        }
        return null;
    }

    @Nullable
    public IntrinsicMethod getIntrinsic(@NotNull CallableMemberDescriptor descriptor) {
        IntrinsicMethod intrinsicMethod = intrinsicsMap.getIntrinsic(descriptor);
        if (intrinsicMethod != null) {
            return intrinsicMethod;
        }

        if (descriptor instanceof SimpleFunctionDescriptor) {
            SimpleFunctionDescriptor functionDescriptor = (SimpleFunctionDescriptor) descriptor;
            if (!functionDescriptor.getReceiverParameter().exists()) {
                FqNameUnsafe ownerFqName = DescriptorUtils.getFQName(descriptor.getContainingDeclaration());
                if (ownerFqName.equalsTo("jet.String")) {
                    return new PsiMethodCall(functionDescriptor);
                }
            }
        }

        List<AnnotationDescriptor> annotations = descriptor.getAnnotations();
        if (annotations != null) {
            for (AnnotationDescriptor annotation : annotations) {
                if("Intrinsic".equals(annotation.getType().getConstructor().getDeclarationDescriptor().getName().getName())) {
                    String value = (String) annotation.getValueArguments().get(0).getValue();
                    intrinsicMethod = namedMethods.get(value);
                    if (intrinsicMethod != null) { break; }
                }
            }
        }
        return intrinsicMethod;
    }

}
