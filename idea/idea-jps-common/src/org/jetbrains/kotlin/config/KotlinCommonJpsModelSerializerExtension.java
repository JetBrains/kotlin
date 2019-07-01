/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension;
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootPropertiesSerializer;

import java.util.Arrays;
import java.util.List;

public class KotlinCommonJpsModelSerializerExtension extends JpsModelSerializerExtension {
    @NotNull
    @Override
    public List<? extends JpsModuleSourceRootPropertiesSerializer<?>> getModuleSourceRootPropertiesSerializers() {
        return Arrays.asList(
                KotlinSourceRootPropertiesSerializer.Source.INSTANCE,
                KotlinSourceRootPropertiesSerializer.TestSource.INSTANCE,
                KotlinResourceRootPropertiesSerializer.Resource.INSTANCE,
                KotlinResourceRootPropertiesSerializer.TestResource.INSTANCE
        );
    }
}