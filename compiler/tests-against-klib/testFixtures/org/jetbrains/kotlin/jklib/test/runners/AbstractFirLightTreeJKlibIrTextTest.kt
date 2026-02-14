package org.jetbrains.kotlin.jklib.test.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Nested

open class AbstractFirLightTreeJKlibIrTextTest : AbstractFirJKlibIrTextTest(FirParser.LightTree)

// We can add a manual runner for specific files if needed, or rely on TestGenerator if we configure it.
// Since we are not running the TestGenerator for this specific class yet, we can manually point to some test files
// or just leave it empty and let the user add @Test methods or use a Runner.
// But usually standard tests are generated.
// To make it usable immediately, I can add a companion object or hardcode some tests, 
// OR I can use @TestMetadata to point to existing tests and hope JUnit picks it up (if using JUnit5 dynamic tests).
// AbstractKotlinCompilerWithTargetBackendTest typically works with generated tests.
