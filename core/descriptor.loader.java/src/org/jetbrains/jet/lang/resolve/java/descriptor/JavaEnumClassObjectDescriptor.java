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

package org.jetbrains.jet.lang.resolve.java.descriptor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptorLite;
import org.jetbrains.jet.lang.resolve.DescriptorFactory;

import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getClassObjectName;

public class JavaEnumClassObjectDescriptor extends MutableClassDescriptorLite {
    private ConstructorDescriptor constructor;

    public JavaEnumClassObjectDescriptor(@NotNull DeclarationDescriptor enumClass) {
        super(enumClass, getClassObjectName(enumClass.getName()), ClassKind.CLASS_OBJECT, false);
    }

    private void initConstructor() {
        if (constructor == null) {
            ConstructorDescriptorImpl constructor = DescriptorFactory.createPrimaryConstructorForObject(this);
            constructor.setReturnType(getDefaultType());
            this.constructor = constructor;
        }
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        initConstructor();
        return Collections.singleton(constructor);
    }

    @Nullable
    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        initConstructor();
        return constructor;
    }
}
