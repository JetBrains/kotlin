/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.test.Assertions;

import java.io.File;

public class RecursiveDescriptorComparatorAdaptor {
    private static final Assertions assertions = JUnit4Assertions.INSTANCE;

    public static void compareDescriptors(
            @NotNull DeclarationDescriptor expected,
            @NotNull DeclarationDescriptor actual,
            @NotNull RecursiveDescriptorComparator.Configuration configuration,
            @Nullable File txtFile
    ) {
        RecursiveDescriptorComparator.compareDescriptors(expected, actual, configuration, txtFile, assertions);
    }

    public static void validateAndCompareDescriptorWithFile(
            @NotNull DeclarationDescriptor actual,
            @NotNull RecursiveDescriptorComparator.Configuration configuration,
            @NotNull File txtFile
    ) {
        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(actual, configuration, txtFile, assertions);
    }

    public static void validateAndCompareDescriptors(
            @NotNull DeclarationDescriptor expected,
            @NotNull DeclarationDescriptor actual,
            @NotNull RecursiveDescriptorComparator.Configuration configuration,
            @Nullable File txtFile
    ) {
        RecursiveDescriptorComparator.validateAndCompareDescriptors(expected, actual, configuration, txtFile, assertions);
    }
}
