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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import jet.typeinfo.TypeInfoVariance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiClassWrapper;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.rt.signature.JetSignatureAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureExceptionsAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureReader;
import org.jetbrains.jet.rt.signature.JetSignatureVisitor;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.KOTLIN;

public final class JavaSupertypeResolver {

    private BindingTrace trace;
    private JavaSemanticServices semanticServices;
    private JavaTypeTransformer typeTransformer;
    private JavaClassResolver classResolver;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setSemanticServices(JavaSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
    }

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    public Collection<JetType> getSupertypes(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull PsiClassWrapper psiClass,
            @NotNull ClassPsiDeclarationProvider classData,
            @NotNull List<TypeParameterDescriptor> typeParameters
    ) {

        List<JetType> result = new ArrayList<JetType>();

        String context = "class " + psiClass.getQualifiedName();

        if (psiClass.getJetClass().signature().length() > 0) {
            readSuperTypes(psiClass, typeParameters, classDescriptor, result, context);
        }
        else {
            TypeVariableResolver typeVariableResolverForSupertypes =
                    TypeVariableResolvers.typeVariableResolverFromTypeParameters(typeParameters, classDescriptor, context);
            transformSupertypeList(result, psiClass.getPsiClass().getExtendsListTypes(), typeVariableResolverForSupertypes);
            transformSupertypeList(result, psiClass.getPsiClass().getImplementsListTypes(), typeVariableResolverForSupertypes);
        }

        reportIncompleteHierarchyForErrorTypes(classDescriptor, result);

        if (result.isEmpty()) {
            addBaseClass(psiClass, classData, classDescriptor, result);
        }
        return result;
    }

    private void readSuperTypes(
            PsiClassWrapper psiClass,
            List<TypeParameterDescriptor> typeParameters,
            ClassDescriptor classDescriptor,
            final List<JetType> result,
            String context
    ) {
        final TypeVariableResolver typeVariableResolver =
                TypeVariableResolvers.typeVariableResolverFromTypeParameters(typeParameters, classDescriptor, context);

        new JetSignatureReader(psiClass.getJetClass().signature()).accept(new JetSignatureExceptionsAdapter() {
            @Override
            public JetSignatureVisitor visitFormalTypeParameter(String name, TypeInfoVariance variance, boolean reified) {
                // TODO: collect
                return new JetSignatureAdapter();
            }

            @Override
            public JetSignatureVisitor visitSuperclass() {
                return new JetTypeJetSignatureReader(semanticServices, KotlinBuiltIns.getInstance(),
                                                     typeVariableResolver) {
                    @Override
                    protected void done(@NotNull JetType jetType) {
                        if (!jetType.equals(KotlinBuiltIns.getInstance().getAnyType())) {
                            result.add(jetType);
                        }
                    }
                };
            }

            @Override
            public JetSignatureVisitor visitInterface() {
                return visitSuperclass();
            }
        });
    }

    private void addBaseClass(
            @NotNull PsiClassWrapper psiClass,
            @NotNull ClassPsiDeclarationProvider classData,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull List<JetType> result
    ) {
        if (classData.getDeclarationOrigin() == KOTLIN
            || DescriptorResolverUtils.OBJECT_FQ_NAME.equalsTo(psiClass.getQualifiedName())
            // TODO: annotations
            || classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
            result.add(KotlinBuiltIns.getInstance().getAnyType());
        }
        else {
            ClassDescriptor object = resolveJavaLangObject();
            if (object != null) {
                result.add(object.getDefaultType());
            }
            else {
                //TODO: hack here
                result.add(KotlinBuiltIns.getInstance().getAnyType());
               // throw new IllegalStateException("Could not resolve java.lang.Object");
            }
        }
    }

    private void reportIncompleteHierarchyForErrorTypes(ClassDescriptor classDescriptor, List<JetType> result) {
        for (JetType supertype : result) {
            if (ErrorUtils.isErrorType(supertype)) {
                trace.record(BindingContext.INCOMPLETE_HIERARCHY, classDescriptor);
            }
        }
    }

    private void transformSupertypeList(
            List<JetType> result,
            PsiClassType[] extendsListTypes,
            TypeVariableResolver typeVariableResolver
    ) {
        for (PsiClassType type : extendsListTypes) {
            PsiClass resolved = type.resolve();
            if (resolved != null) {
                String qualifiedName = resolved.getQualifiedName();
                assert qualifiedName != null;
                if (JvmStdlibNames.JET_OBJECT.getFqName().equalsTo(qualifiedName)) {
                    continue;
                }
            }

            JetType transform = typeTransformer
                    .transformToType(type, TypeUsage.SUPERTYPE, typeVariableResolver);
            if (ErrorUtils.isErrorType(transform)) {
                continue;
            }

            result.add(TypeUtils.makeNotNullable(transform));
        }
    }


    @Nullable
    private ClassDescriptor resolveJavaLangObject() {
        ClassDescriptor clazz = classResolver.resolveClass(DescriptorResolverUtils.OBJECT_FQ_NAME,
                                                           DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
        if (clazz == null) {
            // TODO: warning
        }
        return clazz;
    }
}
