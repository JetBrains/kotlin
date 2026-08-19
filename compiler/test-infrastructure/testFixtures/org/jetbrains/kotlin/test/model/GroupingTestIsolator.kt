/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 * This service is used to determine how tests would be grouped in batches in the grouped test engine.
 * For each test the engine computes tokens from all [GroupingTestIsolator] and then forms groups
 *   which have the same token sets.
 * If at least one token for the test was [BatchToken.Isolated], then the test would run in an isolated batch.
 */
abstract class GroupingTestIsolator(val testServices: TestServices, val affectsFileGenerators: Boolean) : ServicesAndDirectivesContainer {
    abstract fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken

    abstract class BatchToken {
        object Regular : BatchToken()
        object Isolated : BatchToken()
        data class Custom(val name: String) : BatchToken()
    }

    /**
     * Deliberately uncached, despite [computeBatchToken] - and therefore this - potentially being invoked more
     * than once for the same isolator instance while a single multi-module test's structure is still being built:
     * see `ModuleStructureExtractorImpl.escapeModuleNameIfNeeded`, which re-checks isolators (via
     * `shouldIsolateTestInGroupingConfiguration`) against a progressively larger snapshot - one synthetic
     * `TestModuleStructure` per module already parsed - before the test's final structure exists.
     *
     * A cache keyed only by [regex] previously reused whatever `true`/`false` the first such call computed for
     * every later call too, even though each call can see a different, growing set of files: a regex that only
     * matches a module added later would wrongly stay cached as absent, and one that only matched an early,
     * since-superseded snapshot would wrongly stay cached as present. [TestModule.equals] doesn't help distinguish
     * these snapshots either - it compares module *names* only, and the synthetic in-progress module here shares
     * its name with the finished module that eventually replaces it. The safe key would have to capture the
     * exact file content scanned, at which point it costs about as much as the scan it's meant to avoid; a plain
     * substring search over a handful of already-small test-data files per call isn't a hot path worth that.
     */
    protected fun TestModuleStructure.sourceContains(regex: Regex): Boolean {
        return modules.any { module ->
            module.files.any { it.originalContent.contains(regex) }
        }
    }

    protected data class ToggledCheckersToken(val toggledCheckers: Set<String>) : BatchToken()
}
