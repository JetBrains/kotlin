/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.TestOnly;

public class ClassBuilderMode {
    public final boolean generateBodies;
    public final boolean generateMetadata;
    public final boolean generateSourceRetentionAnnotations;
    public final boolean generateMultiFileFacadePartClasses;

    private ClassBuilderMode(
            boolean generateBodies,
            boolean generateMetadata,
            boolean generateSourceRetentionAnnotations,
            boolean generateMultiFileFacadePartClasses
    ) {
        this.generateBodies = generateBodies;
        this.generateMetadata = generateMetadata;
        this.generateSourceRetentionAnnotations = generateSourceRetentionAnnotations;
        this.generateMultiFileFacadePartClasses = generateMultiFileFacadePartClasses;
    }
    
    public static ClassBuilderMode full(boolean generateSourceRetentionAnnotations) {
        return generateSourceRetentionAnnotations ? KAPT : FULL;
    }

    /**
     * Full function bodies
     */
    private final static ClassBuilderMode FULL = new ClassBuilderMode(
            /* bodies = */ true, 
            /* metadata = */ true,
            /* sourceRetention = */ false,
            /* generateMultiFileFacadePartClasses = */ true);

    /**
     * Generating light classes: Only function signatures
     */
    public final static ClassBuilderMode LIGHT_CLASSES = new ClassBuilderMode(
            /* bodies = */ false,
            /* metadata = */ false,
            /* sourceRetention = */ true,
            /* generateMultiFileFacadePartClasses = */ false);
    
    /**
     * Function signatures + metadata (to support incremental compilation with kapt)
     */
    public final static ClassBuilderMode KAPT = new ClassBuilderMode(
            /* bodies = */ false,
            /* metadata = */ true,
            /* sourceRetention = */ true,
            /* generateMultiFileFacadePartClasses = */ false);

    /**
     * Function signatures + metadata (to support incremental compilation with kapt)
     */
    public final static ClassBuilderMode KAPT3 = new ClassBuilderMode(
            /* bodies = */ false,
            /* metadata = */ true,
            /* sourceRetention = */ true,
            /* generateMultiFileFacadePartClasses = */ true);

    @TestOnly
    public final static ClassBuilderMode LIGHT_ANALYSIS_FOR_TESTS = new ClassBuilderMode(
            /* bodies = */ false,
            /* metadata = */ true,
            /* sourceRetention = */ false,
            /* generateMultiFileFacadePartClasses = */ true);
}
