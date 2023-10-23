// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlinx.cinterop.*

// CHECK-OPT-LABEL: define i8* @"kfun:kotlinx.cinterop#interpretOpaquePointed(kotlin.native.internal.NativePtr){}kotlinx.cinterop.NativePointed"(i8* %0)
// CHECK-OPT: call i8* @"kfun:kotlinx.cinterop#<NativePointed-unbox>(kotlin.Any?){}kotlinx.cinterop.NativePointed?"

// CHECK-DEBUG-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
// CHECK-AAPCS-DEBUG: call i1 @IsSubtype(%struct.TypeInfo* %{{[0-9]+}}, %struct.TypeInfo* getelementptr inbounds ({{.*}}@"kclass:kotlinx.cinterop.NativePointed", i32 0, i32 0))
// CHECK-DEFAULTABI-DEBUG: call zeroext i1 @IsSubtype(%struct.TypeInfo* %{{[0-9]+}}, %struct.TypeInfo* @"kclass:kotlinx.cinterop.NativePointed")
// CHECK-WINDOWSX64-DEBUG: call zeroext i1 @IsSubtype(%struct.TypeInfo* %{{[0-9]+}}, %struct.TypeInfo* getelementptr inbounds ({{.*}}@"kclass:kotlinx.cinterop.NativePointed", i32 0, i32 0))
// CHECK-DEBUG-LABEL: epilogue:

@kotlinx.cinterop.ExperimentalForeignApi
fun box(): String = memScoped {
    val var1: NativePointed = alloc(4, 4)
    return if (interpretOpaquePointed(var1.rawPtr) is NativePointed) "OK" else "FAIL"
}
