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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.rt.signature.JetSignatureExceptionsAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureVariance;
import org.jetbrains.jet.rt.signature.JetSignatureVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Stepan Koltsov
 */
public abstract class JetTypeJetSignatureReader extends JetSignatureExceptionsAdapter {
    
    private final JavaSemanticServices javaSemanticServices;
    private final JavaDescriptorResolver javaDescriptorResolver;
    private final JetStandardLibrary jetStandardLibrary;
    private final TypeVariableResolver typeVariableResolver;

    public JetTypeJetSignatureReader(JavaSemanticServices javaSemanticServices, JetStandardLibrary jetStandardLibrary, TypeVariableResolver typeVariableResolver) {
        this.javaSemanticServices = javaSemanticServices;
        this.javaDescriptorResolver = javaSemanticServices.getDescriptorResolver();
        this.jetStandardLibrary = jetStandardLibrary;
        this.typeVariableResolver = typeVariableResolver;
    }
    
    
    private JetType getPrimitiveType(char descriptor, boolean nullable) {
        if (!nullable) {
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                if (jvmPrimitiveType.getJvmLetter() == descriptor) {
                    return jetStandardLibrary.getPrimitiveJetType(jvmPrimitiveType.getPrimitiveType());
                }
            }
            if (descriptor == 'V') {
                return JetStandardClasses.getUnitType();
            }
        } else {
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                if (jvmPrimitiveType.getJvmLetter() == descriptor) {
                    return jetStandardLibrary.getNullablePrimitiveJetType(jvmPrimitiveType.getPrimitiveType());
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
    private boolean nullable;
    private List<TypeProjection> typeArguments;

    @Override
    public void visitClassType(String name, boolean nullable, boolean forceReal) {
        FqName ourName = new FqName(name
                .replace('/', '.')
                .replace('$', '.') // TODO: not sure
            );
        
        this.classDescriptor = null;
        if (this.classDescriptor == null && !forceReal) {
            this.classDescriptor = this.javaSemanticServices.getTypeTransformer().getPrimitiveWrappersClassDescriptorMap().get(ourName.getFqName());
        }

        if (this.classDescriptor == null && ourName.equals(new FqName("java.lang.Object")) && !forceReal) {
            this.classDescriptor = JetStandardClasses.getAny();
        }

        if (classDescriptor == null) {
            // TODO: this is the worst code in Kotlin project
            Matcher matcher = Pattern.compile("jet\\.Function(\\d+)").matcher(ourName.getFqName());
            if (matcher.matches()) {
                classDescriptor = JetStandardClasses.getFunction(Integer.parseInt(matcher.group(1)));
            }
        }
        
        if (classDescriptor == null) {
            Matcher matcher = Pattern.compile("jet\\.Tuple(\\d+)").matcher(ourName.getFqName());
            if (matcher.matches()) {
                classDescriptor = JetStandardClasses.getTuple(Integer.parseInt(matcher.group(1)));
            }
        }


        if (this.classDescriptor == null) {
            this.classDescriptor = javaDescriptorResolver.resolveClass(ourName);
        }

        if (this.classDescriptor == null) {
            throw new IllegalStateException("class not found by name: " + ourName); // TODO: wrong exception
        }
        this.nullable = nullable;
        this.typeArguments = new ArrayList<TypeProjection>();
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
        return new JetTypeJetSignatureReader(javaSemanticServices, jetStandardLibrary, typeVariableResolver) {

            @Override
            protected void done(@NotNull JetType jetType) {
                typeArguments.add(new TypeProjection(parseVariance(variance), jetType));
            }
        };
    }

    @Override
    public JetSignatureVisitor visitArrayType(final boolean nullable) {
        return new JetTypeJetSignatureReader(javaSemanticServices, jetStandardLibrary, typeVariableResolver) {
            @Override
            public void visitBaseType(char descriptor, boolean nullable) {
                JetType primitiveType = getPrimitiveType(descriptor, nullable);
                JetType arrayType;
                if (!nullable) {
                    arrayType = jetStandardLibrary.getPrimitiveArrayJetTypeByPrimitiveJetType(primitiveType);
                } else {
                    arrayType = TypeUtils.makeNullableAsSpecified(jetStandardLibrary.getArrayType(primitiveType), nullable);
                }
                JetTypeJetSignatureReader.this.done(arrayType);
            }

            @Override
            protected void done(@NotNull JetType jetType) {
                JetType arrayType = TypeUtils.makeNullableAsSpecified(jetStandardLibrary.getArrayType(jetType), nullable);
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
        JetType jetType = new JetTypeImpl(
                Collections.<AnnotationDescriptor>emptyList(),
                classDescriptor.getTypeConstructor(),
                nullable,
                typeArguments,
                classDescriptor.getMemberScope(typeArguments));
        done(jetType);
    }
    
    protected abstract void done(@NotNull JetType jetType);
}
