// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB

fun box(): String {
    val referenceArray = arrayOf("OK")
    expectIndexOutOfBounds("reference get, negative index") { referenceArray[-1] }
    expectIndexOutOfBounds("reference set, upper bound") { referenceArray[referenceArray.size] = "FAIL" }

    val primitiveArray = IntArray(1)
    expectIndexOutOfBounds("primitive get, upper bound") { primitiveArray[primitiveArray.size] }
    expectIndexOutOfBounds("primitive set, negative index") { primitiveArray[-1] = 1 }

    val booleanArray = BooleanArray(1)
    expectIndexOutOfBounds("boolean get, negative index") { booleanArray[-1] }
    expectIndexOutOfBounds("boolean set, upper bound") { booleanArray[booleanArray.size] = true }

    return "OK"
}

private inline fun expectIndexOutOfBounds(label: String, operation: () -> Unit) {
    try {
        operation()
        error("Expected IndexOutOfBoundsException for $label")
    } catch (_: IndexOutOfBoundsException) {
    }
}
