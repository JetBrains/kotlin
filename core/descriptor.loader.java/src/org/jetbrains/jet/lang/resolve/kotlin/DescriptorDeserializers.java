/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.kotlin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.descriptors.serialization.descriptors.ConstantDeserializer;
import org.jetbrains.jet.descriptors.serialization.descriptors.Deserializers;

import javax.inject.Inject;

public class DescriptorDeserializers implements Deserializers {

    private AnnotationDescriptorDeserializer annotationDescriptorDeserializer;
    private ConstantDescriptorDeserializer constantDescriptorDeserializer;

    @Inject
    public void setAnnotationDescriptorDeserializer(AnnotationDescriptorDeserializer annotationDescriptorDeserializer) {
        this.annotationDescriptorDeserializer = annotationDescriptorDeserializer;
    }

    @Inject
    public void setConstantDescriptorDeserializer(ConstantDescriptorDeserializer constantDescriptorDeserializer) {
        this.constantDescriptorDeserializer = constantDescriptorDeserializer;
    }

    @NotNull
    @Override
    public AnnotationDeserializer getAnnotationDeserializer() {
        return annotationDescriptorDeserializer;
    }

    @NotNull
    @Override
    public ConstantDeserializer getConstantDeserializer() {
        return constantDescriptorDeserializer;
    }
}
