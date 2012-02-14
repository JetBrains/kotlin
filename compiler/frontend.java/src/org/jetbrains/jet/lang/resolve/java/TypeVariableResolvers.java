/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.ArrayList;

/**
 * @author Stepan Koltsov
 */
public class TypeVariableResolvers {

    public static TypeVariableResolver classTypeVariableResolver(ClassOrNamespaceDescriptor clazz) {
        if (clazz instanceof ClassDescriptor) {
            return new TypeVariableResolverFromTypeDescriptors(((ClassDescriptor) clazz).getTypeConstructor().getParameters(), new TypeVariableResolverFromOuters(clazz.getContainingDeclaration()));
        } else {
            return new TypeVariableResolverFromTypeDescriptors(new ArrayList<TypeParameterDescriptor>(0), null);
        }
    }
    
}
