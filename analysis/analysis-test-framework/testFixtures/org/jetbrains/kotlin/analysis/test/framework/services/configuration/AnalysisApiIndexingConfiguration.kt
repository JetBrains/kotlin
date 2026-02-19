/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.configuration

import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

class AnalysisApiIndexingConfiguration(val binaryLibraryIndexingMode: AnalysisApiBinaryLibraryIndexingMode) : TestService

/**
 * Specifies the indexing behavior of [KotlinDeclarationProvider][org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider]
 * for *test module* binary libraries.
 */
enum class AnalysisApiBinaryLibraryIndexingMode {
    /**
     * Stubs should be built for declarations from binary libraries. These stubs should be indexed. This is necessary when FIR symbols from
     * binary libraries are deserialized from stubs.
     */
    INDEX_STUBS,

    /**
     * Binary library declarations should not be indexed. This is the correct option when FIR symbols from binary libraries are deserialized
     * from class files.
     */
    NO_INDEXING,
}

val TestServices.libraryIndexingConfiguration: AnalysisApiIndexingConfiguration by TestServices.testServiceAccessor()
