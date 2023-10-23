// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

class C<T> {
    fun foo(x: T) = x
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
// CHECK-NOT: Int-box
// CHECK-OPT-NOT: Int-unbox
// CHECK-DEBUG: Int-unbox
// CHECK-LABEL: epilogue:
fun box(): String {
    val c = C<Int>()
    val fooref = c::foo
    return if (fooref(42) == 42)
        "OK"
    else
        "FAIL"
}
