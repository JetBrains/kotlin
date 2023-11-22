This testsuit fir specialBackendChecks on Mac hosts relies on platform libs are installed.
To run testsuit locally, make sure the above is executed first:
- `./gradlew :kotlin-native:platformLibs:macos_arm64Install` or
- `./gradlew :kotlin-native:platformLibs:macos_x64Install`

This testsuit is run differently for K1 and K2 frontends:
- K1/N manual: run `compiler/testData/diagnostics/nativeTests/specialBackendChecks/runtests.sh -language-version 1.9`,
- K2/N manual: run `compiler/testData/diagnostics/nativeTests/specialBackendChecks/runtests.sh`,
- K2/N tests are also run in scope of `FirLightTreeOldFrontendNativeDiagnosticsTestGenerated` and `FirPsiOldFrontendNativeDiagnosticsTestGenerated`.

Reference output for K1/N manual run is provided below.

Reference output for K2/N manual run is slightly different for newly-migrated Fir checks: source lines are displayed.
For not yet migrated checks, the output must be the same as below.

```text
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t1.kt
/tmp/t1.kt:12:5: error: variadic function pointers are not supported
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t10.kt
/tmp/t10.kt:8:5: error: type kotlin.Function1<*, kotlin.Int>  of callback parameter 1 is not supported here: * as 1 parameter type
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t11.kt
/tmp/t11.kt:8:5: error: type kotlin.Function1<in kotlin.Int, kotlin.Int>  of callback parameter 1 is not supported here: in-variance of 1 parameter type
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t12.kt
/tmp/t12.kt:8:5: error: type kotlinx.cinterop.CValue<*>?  of callback parameter 1 is not supported here: must not be nullable
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t13.kt
/tmp/t13.kt:9:5: error: type kotlinx.cinterop.CValue<T of <root>.bar>  of callback parameter 1 is not supported here: must be parameterized with concrete class
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t14.kt
/tmp/t14.kt:10:5: error: type kotlinx.cinterop.CValue<<root>.Z>  of callback parameter 1 is not supported here: not a structure or too complex
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t37.kt
/tmp/t37.kt:8:5: error: subclasses of kotlinx.cinterop.NativePointed cannot have properties with backing fields
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t38.kt
/tmp/t38.kt:8:5: error: subclasses of kotlinx.cinterop.NativePointed cannot have properties with backing fields
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t40.kt
/tmp/t40.kt:10:5: error: kotlinx.cinterop.staticCFunction must take an unbound, non-capturing function or lambda, but captures at:
    /tmp/t40.kt:10:21
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t41.kt
/tmp/t41.kt:9:5: error: kotlinx.cinterop.staticCFunction must take an unbound, non-capturing function or lambda, but captures at:
    /tmp/t41.kt:7:17: x
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t42.kt
/tmp/t42.kt:8:5: error: c function signature element mismatch: expected 'kotlin.Any', got 'kotlin.String'
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t43.kt
/tmp/t43.kt:5:21: error: receiver's type kotlin.Float is not an integer type
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t44.kt
/tmp/t44.kt:5:21: error: type argument kotlin.Float is not an integer type
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t45.kt
/tmp/t45.kt:5:21: error: unable to sign extend kotlin.Int to kotlin.Short
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t46.kt
/tmp/t46.kt:5:21: error: unable to narrow kotlin.Int to kotlin.Long
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t47.kt
/tmp/t47.kt:5:21: error: unable to convert kotlin.Int to kotlin.String
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t60.kt
/tmp/t60.kt:6:5: error: subclasses of kotlinx.cinterop.NativePointed cannot have properties with backing fields
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t63.kt
/tmp/t63.kt:26:17: error: calling suspend functions from `autoreleasepool {}` is prohibited, see https://youtrack.jetbrains.com/issue/KT-50786
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t64.kt
/tmp/t64.kt:29:17: error: calling suspend functions from `autoreleasepool {}` is prohibited, see https://youtrack.jetbrains.com/issue/KT-50786
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t8.kt
/tmp/t8.kt:8:5: error: type kotlin.Function0<*>  of callback parameter 1 is not supported here: * as return type
compiler/testData/diagnostics/nativeTests/specialBackendChecks/cInterop/t9.kt
```