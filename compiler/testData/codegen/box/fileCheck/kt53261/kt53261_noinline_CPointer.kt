// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlinx.cinterop.*

// CHECK-AAPCS-OPT-LABEL: define i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(i8* %0, %struct.ObjHeader* %1)
// CHECK-DEFAULTABI-OPT-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(i8* %0, %struct.ObjHeader* %1)
// CHECK-WINDOWSX64-OPT-LABEL: define zeroext i1 @"kfun:kotlinx.cinterop.CPointer#equals(kotlin.Any?){}kotlin.Boolean"(i8* %0, %struct.ObjHeader* %1)
// CHECK-OPT: call i8* @"kfun:kotlinx.cinterop#<CPointer-unbox>(kotlin.Any?){}kotlinx.cinterop.CPointer<-1:0>?"

// CHECK-DEBUG-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
// CHECK-DEBUG: [[var1:%[0-9]+]] = invoke i8* @"kfun:kotlinx.cinterop#<get-ptr>
// CHECK-DEBUG: [[var2:%[0-9]+]] = invoke i8* @"kfun:kotlinx.cinterop#<get-ptr>
// CHECK-DEBUG: = icmp eq i8* [[var1]], [[var2]]
// CHECK-DEBUG-LABEL: epilogue:

@kotlinx.cinterop.ExperimentalForeignApi
fun box(): String = memScoped {
    val var1: IntVar = alloc()
    val var2: IntVar = alloc()
    return if (var1.ptr == var2.ptr) "FAIL" else "OK"
}
