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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.JavaClassClassResolutionFacade;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassObjectAnnotation;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.java.scope.JavaClassNonStaticMembersScope;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiClassWrapper;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameBase;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;

import javax.inject.Inject;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.DescriptorResolver.createEnumClassObjectValueOfMethod;
import static org.jetbrains.jet.lang.resolve.DescriptorResolver.createEnumClassObjectValuesMethod;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassObjectName;

public final class JavaClassObjectResolver {

    private BindingTrace trace;
    private JavaClassClassResolutionFacade classResolutionFacade;
    private JavaSupertypeResolver supertypesResolver;
    private PsiDeclarationProviderFactory psiDeclarationProviderFactory;

    @Inject
    public void setSupertypesResolver(JavaSupertypeResolver supertypesResolver) {
        this.supertypesResolver = supertypesResolver;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setSemanticServices(JavaClassClassResolutionFacade classResolutionFacade) {
        this.classResolutionFacade = classResolutionFacade;
    }

    @Inject
    public void setPsiDeclarationProviderFactory(PsiDeclarationProviderFactory psiDeclarationProviderFactory) {
        this.psiDeclarationProviderFactory = psiDeclarationProviderFactory;
    }

    @Nullable
    public Result createClassObjectDescriptor(
            @NotNull ClassDescriptor containing,
            @NotNull PsiClass psiClass
    ) {
        DescriptorResolverUtils.checkPsiClassIsNotJet(psiClass);

        if (psiClass.isEnum()) {
            return new Result(createClassObjectDescriptorForEnum(containing, psiClass), null);
        }

        PsiClass classObjectPsiClass = getClassObjectPsiClass(psiClass);
        if (classObjectPsiClass == null) {
            return null;
        }

        return createClassObjectFromPsi(containing, classObjectPsiClass);
    }

    @NotNull
    public Result createClassObjectFromPsi(
            @NotNull ClassDescriptor containing,
            @NotNull PsiClass classObjectPsiClass
    ) {
        String qualifiedName = classObjectPsiClass.getQualifiedName();
        assert qualifiedName != null;
        FqName fqName = new FqName(qualifiedName);
        ClassPsiDeclarationProvider classObjectData = psiDeclarationProviderFactory.createBinaryClassData(classObjectPsiClass);
        ClassDescriptorFromJvmBytecode classObjectDescriptor
                = new ClassDescriptorFromJvmBytecode(containing, ClassKind.CLASS_OBJECT, false);
        classObjectDescriptor.setSupertypes(supertypesResolver.getSupertypes(classObjectDescriptor,
                                                                             new PsiClassWrapper(classObjectPsiClass),
                                                                             classObjectData,
                                                                             Collections.<TypeParameterDescriptor>emptyList()));
        setUpClassObjectDescriptor(classObjectDescriptor, containing, fqName, classObjectData, getClassObjectName(containing.getName()));
        return new Result(classObjectDescriptor, classObjectPsiClass);
    }

    @NotNull
    private ClassDescriptorFromJvmBytecode createClassObjectDescriptorForEnum(
            @NotNull ClassDescriptor containing,
            @NotNull PsiClass psiClass
    ) {
        ClassDescriptorFromJvmBytecode classObjectDescriptor = createSyntheticClassObject(containing, psiClass);

        classObjectDescriptor.getBuilder().addFunctionDescriptor(createEnumClassObjectValuesMethod(classObjectDescriptor, trace));
        classObjectDescriptor.getBuilder().addFunctionDescriptor(createEnumClassObjectValueOfMethod(classObjectDescriptor, trace));

        return classObjectDescriptor;
    }

    @NotNull
    private ClassDescriptorFromJvmBytecode createSyntheticClassObject(
            @NotNull ClassDescriptor containing,
            @NotNull PsiClass psiClass
    ) {
        FqNameUnsafe fqName = DescriptorResolverUtils.getFqNameForClassObject(psiClass);
        ClassDescriptorFromJvmBytecode classObjectDescriptor =
                new ClassDescriptorFromJvmBytecode(containing, ClassKind.CLASS_OBJECT, false);
        ClassPsiDeclarationProvider data = psiDeclarationProviderFactory.createSyntheticClassObjectClassData(psiClass);
        setUpClassObjectDescriptor(classObjectDescriptor, containing, fqName, data, getClassObjectName(containing.getName().getName()));
        return classObjectDescriptor;
    }

    private void setUpClassObjectDescriptor(
            @NotNull ClassDescriptorFromJvmBytecode classObjectDescriptor,
            @NotNull ClassDescriptor containing,
            @NotNull FqNameBase fqName,
            @NotNull ClassPsiDeclarationProvider data,
            @NotNull Name classObjectName
    ) {
        classObjectDescriptor.setName(classObjectName);
        classObjectDescriptor.setModality(Modality.FINAL);
        classObjectDescriptor.setVisibility(containing.getVisibility());
        classObjectDescriptor.setTypeParameterDescriptors(Collections.<TypeParameterDescriptor>emptyList());
        classObjectDescriptor.createTypeConstructor();
        JavaClassNonStaticMembersScope classMembersScope = new JavaClassNonStaticMembersScope(classObjectDescriptor, data, classResolutionFacade);
        WritableScopeImpl writableScope =
                new WritableScopeImpl(classMembersScope, classObjectDescriptor, RedeclarationHandler.THROW_EXCEPTION, fqName.toString());
        writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        classObjectDescriptor.setScopeForMemberLookup(writableScope);
        classObjectDescriptor.setScopeForConstructorResolve(classMembersScope);
    }


    @Nullable
    private static PsiClass getClassObjectPsiClass(@NotNull PsiClass ownerClass) {
        if (!DescriptorResolverUtils.isKotlinClass(ownerClass)) {
            return null;
        }

        for (PsiClass inner : ownerClass.getInnerClasses()) {
            if (JetClassObjectAnnotation.get(inner).isDefined()) {
                return inner;
            }
        }
        return null;
    }

    public static final class Result {
        private final ClassDescriptorFromJvmBytecode classObjectDescriptor;
        private final PsiClass classObjectPsiClass;

        public Result(@NotNull ClassDescriptorFromJvmBytecode classObjectDescriptor, @Nullable PsiClass classObjectPsiClass) {
            this.classObjectDescriptor = classObjectDescriptor;
            this.classObjectPsiClass = classObjectPsiClass;
        }

        @NotNull
        public ClassDescriptorFromJvmBytecode getClassObjectDescriptor() {
            return classObjectDescriptor;
        }

        @Nullable
        public PsiClass getClassObjectPsiClass() {
            return classObjectPsiClass;
        }
    }
}
