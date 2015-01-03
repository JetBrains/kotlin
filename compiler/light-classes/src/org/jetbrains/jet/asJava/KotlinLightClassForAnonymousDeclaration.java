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

package org.jetbrains.jet.asJava;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

class KotlinLightClassForAnonymousDeclaration extends KotlinLightClassForExplicitDeclaration implements PsiAnonymousClass {
    private static final Logger LOG = Logger.getInstance(KotlinLightClassForAnonymousDeclaration.class);

    private SoftReference<PsiClassType> cachedBaseType = null;

    KotlinLightClassForAnonymousDeclaration(@NotNull PsiManager manager, @NotNull FqName name, @NotNull JetClassOrObject classOrObject) {
        super(manager, name, classOrObject);
    }

    @NotNull
    @Override
    public PsiJavaCodeReferenceElement getBaseClassReference() {
        Project project = classOrObject.getProject();
        return JavaPsiFacade.getElementFactory(project).createReferenceElementByFQClassName(
                getFirstSupertypeFQName(), GlobalSearchScope.allScope(project)
        );
    }

    private String getFirstSupertypeFQName() {
        ClassDescriptor descriptor = getDescriptor();
        if (descriptor == null) return CommonClassNames.JAVA_LANG_OBJECT;

        Collection<JetType> superTypes = descriptor.getTypeConstructor().getSupertypes();

        if (superTypes.isEmpty()) return CommonClassNames.JAVA_LANG_OBJECT;

        JetType superType = superTypes.iterator().next();
        DeclarationDescriptor superClassDescriptor = superType.getConstructor().getDeclarationDescriptor();

        if (superClassDescriptor == null) {
            LOG.error("No declaration descriptor for supertype " + superType + " of " + getDescriptor());
            // return java.lang.Object for recovery
            return CommonClassNames.JAVA_LANG_OBJECT;
        }

        return DescriptorUtils.getFqName(superClassDescriptor).asString();
    }

    @NotNull
    @Override
    public synchronized PsiClassType getBaseClassType() {
        PsiClassType type = null;
        if (cachedBaseType != null) type = cachedBaseType.get();
        if (type != null && type.isValid()) return type;

        type = JavaPsiFacade.getInstance(classOrObject.getProject()).getElementFactory().createType(getBaseClassReference());
        cachedBaseType = new SoftReference<PsiClassType>(type);
        return type;
    }

    @Nullable
    @Override
    public PsiExpressionList getArgumentList() {
        return null;
    }

    @Override
    public boolean isInQualifiedNew() {
        return false;
    }
}
