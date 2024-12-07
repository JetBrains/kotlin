// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun foo(arr: Array<Int>?) {
    for (x in arr!!) {
        checkSubtype<Array<Int>>(arr)
    }
    checkSubtype<Array<Int>>(arr)
}
