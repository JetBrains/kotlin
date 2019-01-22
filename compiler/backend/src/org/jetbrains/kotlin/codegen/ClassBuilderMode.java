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
    public final boolean mightBeIncorrectCode;

    private ClassBuilderMode(
            boolean generateBodies,
            boolean generateMetadata,
            boolean generateSourceRetentionAnnotations,
            boolean generateMultiFileFacadePartClasses,
            boolean mightBeIncorrectCode
    ) {
        this.generateBodies = generateBodies;
        this.generateMetadata = generateMetadata;
        this.generateSourceRetentionAnnotations = generateSourceRetentionAnnotations;
        this.generateMultiFileFacadePartClasses = generateMultiFileFacadePartClasses;
        this.mightBeIncorrectCode = mightBeIncorrectCode;
    }

    /**
     * Full function bodies
     */
    public final static ClassBuilderMode FULL = new ClassBuilderMode(
            /* bodies = */ true,
            /* metadata = */ true,
            /* sourceRetention = */ false,
            /* generateMultiFileFacadePartClasses = */ true,
            /* mightBeIncorrectCode = */ false);

    /**
     * ABI for compilation (non-private signatures + inline function bodies)
     */
    public final static ClassBuilderMode ABI = new ClassBuilderMode(
            /* bodies = */ true,
            /* metadata = */ true,
            /* sourceRetention = */ false,
            /* generateMultiFileFacadePartClasses = */ true,
            /* mightBeIncorrectCode = */ false);

    /**
     * Generating light classes: Only function signatures
     */
    public final static ClassBuilderMode LIGHT_CLASSES = new ClassBuilderMode(
            /* bodies = */ false,
            /* metadata = */ false,
            /* sourceRetention = */ true,
            /* generateMultiFileFacadePartClasses = */ false,
            /* mightBeIncorrectCode = */ true);

    /**
     * Function signatures + metadata (to support incremental compilation with kapt)
     */
    public final static ClassBuilderMode KAPT3 = new ClassBuilderMode(
            /* bodies = */ false,
            /* metadata = */ true,
            /* sourceRetention = */ true,
            /* generateMultiFileFacadePartClasses = */ true,
            /* mightBeIncorrectCode = */ true);

    private final static ClassBuilderMode LIGHT_ANALYSIS_FOR_TESTS = new ClassBuilderMode(
            /* bodies = */ false,
            /* metadata = */ true,
            /* sourceRetention = */ false,
            /* generateMultiFileFacadePartClasses = */ true,
            /* mightBeIncorrectCode = */ true);

    @TestOnly
    public static ClassBuilderMode getLightAnalysisForTests() {
        return LIGHT_ANALYSIS_FOR_TESTS;
    }
}
