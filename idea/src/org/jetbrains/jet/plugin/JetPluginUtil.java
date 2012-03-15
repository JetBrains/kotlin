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

package org.jetbrains.jet.plugin;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.LinkedList;

/**
 * @author svtk
 */
public class JetPluginUtil {
    @NotNull
    public static FqName computeTypeFullName(@NotNull JetType type) {
        ClassDescriptor clazz = (ClassDescriptor) type.getConstructor().getDeclarationDescriptor();
        return DescriptorUtils.getFQName(clazz).toSafe();
    }

    @NotNull
    private static LinkedList<String> computeTypeFullNameList(JetType type) {
        if (type instanceof DeferredType) {
            type = ((DeferredType)type).getActualType();
        }
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();

        LinkedList<String> fullName = Lists.newLinkedList();
        while (declarationDescriptor != null && !(declarationDescriptor instanceof ModuleDescriptor)) {
            fullName.addFirst(declarationDescriptor.getName());
            declarationDescriptor = declarationDescriptor.getContainingDeclaration();
        }
        assert fullName.size() > 0;
        if (JavaDescriptorResolver.JAVA_ROOT.equals(fullName.getFirst())) {
            fullName.removeFirst();
        }
        return fullName;
    }

    public static boolean checkTypeIsStandard(JetType type, Project project) {
        if (JetStandardClasses.isAny(type) || JetStandardClasses.isNothingOrNullableNothing(type) || JetStandardClasses.isUnit(type) ||
            JetStandardClasses.isTupleType(type) || JetStandardClasses.isFunctionType(type)) {
            return true;
        }

        LinkedList<String> fullName = computeTypeFullNameList(type);
        if (fullName.size() == 3 && fullName.getFirst().equals("java") && fullName.get(1).equals("lang")) {
            return true;
        }

        JetStandardLibrary standardLibrary = JetStandardLibrary.getInstance();
        JetScope libraryScope = standardLibrary.getLibraryScope();

        DeclarationDescriptor declaration = type.getMemberScope().getContainingDeclaration();
        if (ErrorUtils.isError(declaration)) {
            return false;
        }
        while (!(declaration instanceof NamespaceDescriptor)) {
            declaration = declaration.getContainingDeclaration();
            assert declaration != null;
        }
        return libraryScope == ((NamespaceDescriptor) declaration).getMemberScope();
    }
}
