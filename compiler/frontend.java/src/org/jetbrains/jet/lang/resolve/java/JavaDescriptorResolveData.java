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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.ClassDescriptorFromJvmBytecode;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageScope;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Map;

public class JavaDescriptorResolveData {
    public static abstract class ResolverScopeData {
        @Nullable
        public PsiClass getPsiClass() {
            return psiClass;
        }

        @Nullable
        public PsiPackage getPsiPackage() {
            return psiPackage;
        }

        @Nullable
        public FqName getFqName() {
            return fqName;
        }

        public boolean isStaticMembers() {
            return staticMembers;
        }

        public boolean isKotlin() {
            return kotlin;
        }

        public ClassOrNamespaceDescriptor getClassOrNamespaceDescriptor() {
            return classOrNamespaceDescriptor;
        }

        @Nullable
        private final PsiClass psiClass;
        @Nullable
        private final PsiPackage psiPackage;
        @Nullable
        private final FqName fqName;
        private final boolean staticMembers;
        private final boolean kotlin;
        private final ClassOrNamespaceDescriptor classOrNamespaceDescriptor;

        protected ResolverScopeData(
                @Nullable PsiClass psiClass,
                @Nullable PsiPackage psiPackage,
                @Nullable FqName fqName,
                boolean staticMembers,
                @NotNull ClassOrNamespaceDescriptor descriptor
        ) {
            JavaDescriptorResolver.checkPsiClassIsNotJet(psiClass);

            this.psiClass = psiClass;
            this.psiPackage = psiPackage;
            this.fqName = fqName;

            if (psiClass == null && psiPackage == null) {
                throw new IllegalStateException("both psiClass and psiPackage cannot be null");
            }

            this.staticMembers = staticMembers;
            this.kotlin = psiClass != null && JavaDescriptorResolver.isKotlinClass(psiClass);
            classOrNamespaceDescriptor = descriptor;

            if (fqName != null && fqName.lastSegmentIs(Name.identifier(JvmAbi.PACKAGE_CLASS)) && psiClass != null && kotlin) {
                throw new IllegalStateException("Kotlin namespace cannot have last segment " + JvmAbi.PACKAGE_CLASS + ": " + fqName);
            }
        }

        protected ResolverScopeData(boolean negative) {
            if (!negative) {
                throw new IllegalStateException();
            }
            this.psiClass = null;
            this.psiPackage = null;
            this.fqName = null;
            this.staticMembers = false;
            this.kotlin = false;
            this.classOrNamespaceDescriptor = null;
        }

        @NotNull
        public PsiElement getPsiPackageOrPsiClass() {
            if (psiPackage != null) {
                return psiPackage;
            }
            else {
                assert psiClass != null;
                return psiClass;
            }
        }

        private Map<Name, NamedMembers> namedMembersMap;

        Map<Name, NamedMembers> getNamedMembersMap() {
            return namedMembersMap;
        }

        void setNamedMembersMap(Map<Name, NamedMembers> namedMembersMap) {
            this.namedMembersMap = namedMembersMap;
        }
    }

    /**
     * Class with instance members
     */
    public static class ResolverBinaryClassData extends ResolverClassData {

        public ResolverBinaryClassData(
                @NotNull PsiClass psiClass,
                @Nullable FqName fqName,
                @NotNull ClassDescriptorFromJvmBytecode classDescriptor
        ) {
            super(psiClass, null, fqName, false, classDescriptor);
        }

        ResolverBinaryClassData(boolean negative) {
            super(negative);
        }

        static final ResolverClassData NEGATIVE = new ResolverBinaryClassData(true);
    }

    public static class ResolverClassData extends ResolverScopeData {

        private final ClassDescriptorFromJvmBytecode classDescriptor;

        protected ResolverClassData(boolean negative) {
            super(negative);
            this.classDescriptor = null;
        }

        protected ResolverClassData(
                @Nullable PsiClass psiClass,
                @Nullable PsiPackage psiPackage,
                @Nullable FqName fqName,
                boolean staticMembers,
                @NotNull ClassDescriptorFromJvmBytecode descriptor
        ) {
            super(psiClass, psiPackage, fqName, staticMembers, descriptor);
            classDescriptor = descriptor;
        }


        public ClassDescriptorFromJvmBytecode getClassDescriptor() {
            return classDescriptor;
        }
    }


    static class ResolverSyntheticClassObjectClassData extends ResolverClassData {

        protected ResolverSyntheticClassObjectClassData(
                @Nullable PsiClass psiClass,
                @Nullable FqName fqName,
                @NotNull ClassDescriptorFromJvmBytecode descriptor
        ) {
            super(psiClass, null, fqName, true, descriptor);
        }
    }

    /**
     * Either package or class with static members
     */
    static class ResolverNamespaceData extends ResolverScopeData {
        static final ResolverNamespaceData NEGATIVE = new ResolverNamespaceData(true);

        private final NamespaceDescriptor namespaceDescriptor;

        private JavaPackageScope memberScope;

        ResolverNamespaceData(
                @Nullable PsiClass psiClass,
                @Nullable PsiPackage psiPackage,
                @NotNull FqName fqName,
                @NotNull NamespaceDescriptor namespaceDescriptor
        ) {
            super(psiClass, psiPackage, fqName, true, namespaceDescriptor);
            this.namespaceDescriptor = namespaceDescriptor;
        }

        private ResolverNamespaceData(boolean negative) {
            super(negative);
            this.namespaceDescriptor = null;
        }

        JavaPackageScope getMemberScope() {
            return memberScope;
        }

        public NamespaceDescriptor getNamespaceDescriptor() {
            return namespaceDescriptor;
        }

        public void setMemberScope(JavaPackageScope memberScope) {
            this.memberScope = memberScope;
        }
    }
}
