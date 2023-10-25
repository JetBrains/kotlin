// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-AAPCS-OPT-LABEL: define i1 @"kfun:kotlin.UIntArray#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)
// CHECK-DEFAULTABI-OPT-LABEL: define zeroext i1 @"kfun:kotlin.UIntArray#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)
// CHECK-WINDOWSX64-OPT-LABEL: define zeroext i1 @"kfun:kotlin.UIntArray#equals(kotlin.Any?){}kotlin.Boolean"(%struct.ObjHeader* %0, %struct.ObjHeader* %1)

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"

// CHECK-OPT: call %struct.ObjHeader* @"kfun:kotlin#<UIntArray-unbox>(kotlin.Any?){}kotlin.UIntArray?"

// CHECK-LABEL: epilogue:

fun box(): String {
    val arr1 = UIntArray(10) { it.toUInt() }
    val arr2 = UIntArray(10) { (it / 2).toUInt() }
    return if (arr1 == arr2) "FAIL" else "OK"
}
