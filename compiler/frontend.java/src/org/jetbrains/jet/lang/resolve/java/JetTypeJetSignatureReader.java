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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.rt.signature.JetSignatureExceptionsAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureVariance;
import org.jetbrains.jet.rt.signature.JetSignatureVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class JetTypeJetSignatureReader extends JetSignatureExceptionsAdapter {
    
    private final JavaSemanticServices javaSemanticServices;
    private final JavaDescriptorResolver javaDescriptorResolver;
    private final KotlinBuiltIns kotlinBuiltIns;
    private final TypeVariableResolver typeVariableResolver;

    public JetTypeJetSignatureReader(JavaSemanticServices javaSemanticServices, KotlinBuiltIns kotlinBuiltIns, TypeVariableResolver typeVariableResolver) {
        this.javaSemanticServices = javaSemanticServices;
        this.javaDescriptorResolver = javaSemanticServices.getDescriptorResolver();
        this.kotlinBuiltIns = kotlinBuiltIns;
        this.typeVariableResolver = typeVariableResolver;
    }
    
    
    private JetType getPrimitiveType(char descriptor, boolean nullable) {
        if (!nullable) {
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                if (jvmPrimitiveType.getJvmLetter() == descriptor) {
                    return KotlinBuiltIns.getInstance().getPrimitiveJetType(jvmPrimitiveType.getPrimitiveType());
                }
            }
            if (descriptor == 'V') {
                return KotlinBuiltIns.getInstance().getUnitType();
            }
        }
        else {
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                if (jvmPrimitiveType.getJvmLetter() == descriptor) {
                    return KotlinBuiltIns.getInstance().getNullablePrimitiveJetType(jvmPrimitiveType.getPrimitiveType());
                }
            }
            if (descriptor == 'V') {
                throw new IllegalStateException("incorrect signature: nullable void");
            }
        }
        throw new IllegalStateException("incorrect signature");
    }

    @Override
    public void visitBaseType(char descriptor, boolean nullable) {
        done(getPrimitiveType(descriptor, nullable));
    }


    private ClassDescriptor classDescriptor;
    private JetType errorType;
    private boolean nullable;
    private List<TypeProjection> typeArguments;

    @Override
    public void visitClassType(String signatureName, boolean nullable, boolean forceReal) {
        FqName fqName = JvmClassName.bySignatureName(signatureName).getFqName();

        enterClass(resolveClassDescriptorByFqName(fqName, forceReal), fqName.getFqName(), nullable);
    }

    private void enterClass(@Nullable ClassDescriptor classDescriptor, @NotNull String className, boolean nullable) {
        this.classDescriptor = classDescriptor;

        if (this.classDescriptor == null) {
            // TODO: report in to trace
            this.errorType = ErrorUtils.createErrorType("class not found by name: " + className);
        }
        this.nullable = nullable;
        this.typeArguments = new ArrayList<TypeProjection>();
    }

    @Nullable
    private ClassDescriptor resolveClassDescriptorByFqName(FqName ourName, boolean forceReal) {
        if (!forceReal) {
            ClassDescriptor mappedDescriptor = JavaToKotlinClassMap.getInstance().
                    mapKotlinClass(ourName, TypeUsage.MEMBER_SIGNATURE_INVARIANT);
            if (mappedDescriptor != null) {
                return mappedDescriptor;
            }
        }

        return javaDescriptorResolver.resolveClass(ourName, DescriptorSearchRule.INCLUDE_KOTLIN);
    }

    @Override
    public void visitInnerClassType(String signatureName, boolean nullable, boolean forceReal) {
        JvmClassName jvmClassName = JvmClassName.bySignatureName(signatureName);
        ClassDescriptor descriptor = resolveClassDescriptorByFqName(jvmClassName.getOuterClassFqName(), forceReal);
        for (String innerClassName : jvmClassName.getInnerClassNameList()) {
            descriptor = descriptor != null ? DescriptorUtils.getInnerClassByName(descriptor, innerClassName) : null;
        }
        enterClass(descriptor, signatureName, nullable);
    }

    private static Variance parseVariance(JetSignatureVariance variance) {
        switch (variance) {
            case INVARIANT: return Variance.INVARIANT;
            case OUT: return Variance.OUT_VARIANCE;
            case IN: return Variance.IN_VARIANCE;
            default: throw new IllegalStateException();
        }
    }

    @Override
    public JetSignatureVisitor visitTypeArgument(final JetSignatureVariance variance) {
        return new JetTypeJetSignatureReader(javaSemanticServices, kotlinBuiltIns, typeVariableResolver) {

            @Override
            protected void done(@NotNull JetType jetType) {
                typeArguments.add(new TypeProjection(parseVariance(variance), jetType));
            }
        };
    }

    @Override
    public JetSignatureVisitor visitArrayType(final boolean nullable, final JetSignatureVariance wildcard) {
        return new JetTypeJetSignatureReader(javaSemanticServices, kotlinBuiltIns, typeVariableResolver) {
            @Override
            public void visitBaseType(char descriptor, boolean nullable) {
                JetType primitiveType = getPrimitiveType(descriptor, nullable);
                JetType arrayType;
                if (!nullable) {
                    arrayType = KotlinBuiltIns.getInstance().getPrimitiveArrayJetTypeByPrimitiveJetType(primitiveType);
                }
                else {
                    arrayType = TypeUtils.makeNullableAsSpecified(KotlinBuiltIns.getInstance().getArrayType(primitiveType), nullable);
                }
                JetTypeJetSignatureReader.this.done(arrayType);
            }

            @Override
            protected void done(@NotNull JetType jetType) {
                JetType arrayType = TypeUtils.makeNullableAsSpecified(KotlinBuiltIns.getInstance().getArrayType(
                        parseVariance(wildcard), jetType), nullable);
                JetTypeJetSignatureReader.this.done(arrayType);
            }
        };
    }

    @Override
    public void visitTypeVariable(String name, boolean nullable) {
        JetType r = TypeUtils.makeNullableAsSpecified(typeVariableResolver.getTypeVariable(name).getDefaultType(), nullable);
        done(r);
    }

    @Override
    public void visitEnd() {
        if ((errorType != null) == (classDescriptor != null)) {
            throw new IllegalStateException("must initialize either errorType or classDescriptor");
        }
        JetType jetType;
        if (errorType != null) {
            jetType = errorType;
        }
        else {
            jetType = new JetTypeImpl(
                    Collections.<AnnotationDescriptor>emptyList(),
                    classDescriptor.getTypeConstructor(),
                    nullable,
                    typeArguments,
                    classDescriptor.getMemberScope(typeArguments));
        }
        done(jetType);
    }
    
    protected abstract void done(@NotNull JetType jetType);
}
