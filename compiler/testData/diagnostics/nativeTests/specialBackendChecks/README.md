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
/tmp/t9.kt:8:5: error: type kotlin.Function0<out kotlin.Int>  of callback parameter 1 is not supported here: out-variance of return type
compiler/testData/diagnostics/nativeTests/specialBackendChecks/concurrent/t48.kt
/tmp/t48.kt:11:12: error: kotlin.native.concurrent.Worker.execute must take an unbound, non-capturing function or lambda, but captures at:
    /tmp/t48.kt:11:50
compiler/testData/diagnostics/nativeTests/specialBackendChecks/concurrent/t49.kt
/tmp/t49.kt:7:12: error: kotlin.native.concurrent.Worker.execute must take an unbound, non-capturing function or lambda, but captures at:
    /tmp/t49.kt:7:61: x
compiler/testData/diagnostics/nativeTests/specialBackendChecks/concurrent/t50.kt
/tmp/t50.kt:12:16: error: kotlin.native.concurrent.Worker.execute must take an unbound, non-capturing function or lambda, but captures at:
    /tmp/t50.kt:12:54
compiler/testData/diagnostics/nativeTests/specialBackendChecks/concurrent/t51.kt
/tmp/t51.kt:10:28: error: kotlin.native.concurrent.Worker.execute must take an unbound, non-capturing function or lambda, but captures at:
    /tmp/t51.kt:10:66
compiler/testData/diagnostics/nativeTests/specialBackendChecks/immutableBlobOf/t54.kt
/tmp/t54.kt:4:44: error: no spread elements allowed here
compiler/testData/diagnostics/nativeTests/specialBackendChecks/immutableBlobOf/t55.kt
/tmp/t55.kt:4:37: error: all elements of binary blob must be constants
compiler/testData/diagnostics/nativeTests/specialBackendChecks/immutableBlobOf/t56.kt
/tmp/t56.kt:4:29: error: incorrect value for binary data: 1000
compiler/testData/diagnostics/nativeTests/specialBackendChecks/immutableBlobOf/t57.kt
/tmp/t57.kt:4:13: error: expected at least one element
compiler/testData/diagnostics/nativeTests/specialBackendChecks/nativeRef/t52.kt
/tmp/t52.kt:6:5: error: kotlin.native.ref.createCleaner must take an unbound, non-capturing function or lambda, but captures at:
    /tmp/t52.kt:6:33: x
compiler/testData/diagnostics/nativeTests/specialBackendChecks/nativeRef/t53.kt
/tmp/t53.kt:10:5: error: kotlin.native.ref.createCleaner must take an unbound, non-capturing function or lambda, but captures at:
    /tmp/t53.kt:10:23
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t15.kt
/tmp/t15.kt:6:26: error: type kotlin.Function0<kotlin.Unit>  is not supported here: not supported as variadic argument
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t16.kt
/tmp/t16.kt:8:26: error: type <root>.Z  is not supported here: doesn't correspond to any C type
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t17.kt
/tmp/t17.kt:8:15: error: super calls to Objective-C protocols are not allowed
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t18.kt
/tmp/t18.kt:8:19: error: super calls to Objective-C meta classes are not supported yet
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t2.kt
/tmp/t2.kt:8:5: error: 'handleFailureInFunction' overrides nothing
    override fun handleFailureInFunction(functionName: String, file: String, lineNumber: NSInteger /* = Long */, description: String?, vararg args: Any?) { }
    ^
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t20.kt
/tmp/t20.kt:6:1: error: only classes are supported as subtypes of Objective-C types
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t21.kt
/tmp/t21.kt:6:1: error: non-final Kotlin subclasses of Objective-C classes are not yet supported
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t22.kt
/tmp/t22.kt:6:1: error: fields are not supported for Companion of subclass of ObjC type
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t23.kt
/tmp/t23.kt:8:1: error: mixing Kotlin and Objective-C supertypes is not supported
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t24.kt
/tmp/t24.kt:6:1: error: only companion objects of subclasses of Objective-C classes can inherit from Objective-C metaclasses
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t25.kt
/tmp/t25.kt:7:14: error: can't override 'toString', override 'description' instead
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t26.kt
/tmp/t26.kt:11:9: error: @kotlinx.cinterop.ObjCAction method must not have extension receiver
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t27.kt
/tmp/t27.kt:11:13: error: unexpected @kotlinx.cinterop.ObjCAction method parameter type: kotlin.String
Only Objective-C object types are supported here
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t28.kt
/tmp/t28.kt:10:5: error: unexpected @kotlinx.cinterop.ObjCAction method return type: kotlin.Int
Only 'Unit' is supported here
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t29.kt
/tmp/t29.kt:9:5: error: @kotlinx.cinterop.ObjCOutlet property must be var
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t30.kt
/tmp/t30.kt:10:9: error: @kotlinx.cinterop.ObjCOutlet must not have extension receiver
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t31.kt
/tmp/t31.kt:8:5: error: unexpected @kotlinx.cinterop.ObjCOutlet type: kotlin.String
Only Objective-C object types are supported here
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t32.kt
/tmp/t32.kt:10:5: error: constructor with @kotlinx.cinterop.ObjCObjectBase.OverrideInit doesn't override any super class constructor.
It must completely match by parameter names and types.
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t33.kt
/tmp/t33.kt:10:5: error: constructor with @kotlinx.cinterop.ObjCObjectBase.OverrideInit overrides initializer that is already overridden explicitly
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t34.kt
/tmp/t34.kt:10:5: error: only 0, 1 or 2 parameters are supported here
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t35.kt
/tmp/t35.kt:7:13: error: unable to call non-designated initializer as super constructor
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t4.kt
/tmp/t4.kt:6:21: error: callable references to variadic Objective-C methods are not supported
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t5.kt
/tmp/t5.kt:6:83: error: passing String as variadic Objective-C argument is ambiguous; cast it to NSString or pass with '.cstr' as C string
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t6.kt
/tmp/t6.kt:6:97: error: when calling variadic Objective-C methods spread operator is supported only for *arrayOf(...)
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t61.kt
/tmp/t61.kt:5:5: error: only companion objects of subclasses of Objective-C classes can inherit from Objective-C metaclasses
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t62.kt
/tmp/t62.kt:4:1: error: only companion objects of subclasses of Objective-C classes can inherit from Objective-C metaclasses
compiler/testData/diagnostics/nativeTests/specialBackendChecks/objCInterop/t7.kt
/tmp/t7.kt:6:41: error: when calling variadic C functions spread operator is supported only for *arrayOf(...)
compiler/testData/diagnostics/nativeTests/specialBackendChecks/posix/t3.kt
/tmp/t3.kt:6:13: error: callable references to variadic C functions are not supported
compiler/testData/diagnostics/nativeTests/specialBackendChecks/posix/t36.kt
/tmp/t36.kt:7:13: error: native interop types constructors must not be called directly
compiler/testData/diagnostics/nativeTests/specialBackendChecks/reflect/t58.kt
/tmp/t58.kt:6:5: error: non-reified type parameters with recursive bounds are not supported yet: TYPE_PARAMETER name:T index:0 variance: superTypes:[kotlin.Comparable<T of <root>.foo>] reified:false
compiler/testData/diagnostics/nativeTests/specialBackendChecks/reflect/t59.kt
/tmp/t59.kt:6:5: error: non-reified type parameters with recursive bounds are not supported yet: TYPE_PARAMETER name:T index:0 variance: superTypes:[kotlin.Comparable<T of <root>.foo>] reified:false
```