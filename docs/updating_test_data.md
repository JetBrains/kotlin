If you need to update all test data in bulk, such as after changing a format of some test data, `kotlin.test.update.test.data=true` JVM property will be useful, see also [compiler/tests-common-new/tests/org/jetbrains/kotlin/test/backend/handlers/UpdateTestDataHandler.kt].

The script below will try to update all the test data in the repository. If you are lucky, it will be up-to-date when you are reading this, otherwise you will need to adjust it for a changes in test organization.

It was tested on updating IR dump files (`*.ir.txt` and `*.kt.txt`), it may not necessarily cover other test files.

```
./gradlew :compiler:tests-common-new:test --tests "org.jetbrains.kotlin.test.runners.ir.ClassicJvmIrTextTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :compiler:tests-common-new:test --tests "org.jetbrains.kotlin.test.runners.ir.ClassicJvmIrSourceRangesTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :compiler:tests-common-new:test --tests "org.jetbrains.kotlin.test.runners.codegen.IrBlackBoxCodegenTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :compiler:tests-common-new:test --tests "org.jetbrains.kotlin.test.runners.codegen.BlackBoxModernJdkCodegenTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew  :compiler:tests-common-new:test --tests "org.jetbrains.kotlin.test.runners.codegen.IrBytecodeListingTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.ir.FirLightTreeJvmIrTextTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.ir.FirLightTreeJvmIrSourceRangesTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.codegen.FirLightTreeBlackBoxCodegenTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.codegen.FirLightTreeBlackBoxModernJdkCodegenTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.codegen.FirLightTreeBytecodeListingTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :js:js.tests:test --tests "org.jetbrains.kotlin.js.test.ir.ClassicJsIrTextTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :js:js.tests:test --tests "org.jetbrains.kotlin.js.test.fir.FirLightTreeJsIrTextTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :native:native.tests:test --tests "org.jetbrains.kotlin.konan.test.irtext.ClassicNativeIrTextTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :native:native.tests:test --tests "org.jetbrains.kotlin.konan.test.irtext.FirLightTreeNativeIrTextTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :native:native.tests:klibTest --tests "org.jetbrains.kotlin.konan.test.klib.FirKlibCrossCompilationIdentityTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :native:native.tests:klib-ir-inliner:test --tests "org.jetbrains.kotlin.konan.test.dump.FirNativeKlibDumpIrTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :compiler:test --tests "org.jetbrains.kotlin.codegen.ScriptCodegenTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :compiler:fir:fir2ir:test --tests "org.jetbrains.kotlin.test.runners.codegen.FirScriptCodegenTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :analysis:analysis-api-fir:test --tests "org.jetbrains.kotlin.analysis.api.fir.test.cases.generated.cases.components.compilerFacility.*" --continue -Pkotlin.test.update.test.data=true
./gradlew :plugins:plugin-sandbox:test --tests "org.jetbrains.kotlin.plugin.sandbox.FirLightTreePluginBlackBoxCodegenTestGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :kotlinx-serialization-compiler-plugin:test --tests "org.jetbrains.kotlinx.serialization.runners.CompilerFacilityTestForSerializationGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :kotlin-power-assert-compiler-plugin:test --tests "org.jetbrains.kotlin.powerassert.IrBlackBoxCodegenTestForPowerAssertGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :kotlin-power-assert-compiler-plugin:test --tests "org.jetbrains.kotlin.powerassert.FirLightTreeBlackBoxCodegenTestForPowerAssertGenerated" --continue -Pkotlin.test.update.test.data=true
./gradlew :plugins:compose-compiler-plugin:compiler-hosted:test --tests "androidx.compose.compiler.plugins.kotlin.CompilerFacilityTestForComposeCompilerPluginGenerated" --continue -Pkotlin.test.update.test.data=true
```