// WITH_RUNTIME

fun box(): String {
    for (element in 5.toByte()..1.toByte() step 255) {
        return "Fail: iterating over an empty progression, element: $element"
    }

    return "OK"
}
