// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * MAIN LINK: expressions, equality-expressions, reference-equality-expressions -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: If these values are non-equal by value, they are also non-equal by reference;
 */

fun box(): String {
    val a1 = A(false)
    val a2 = A(true)

    if (!a1.equals( a2)) {
        if (a1 !== a2)
            return "OK"
        else throw Exception()
    }
    return "NOK"
}

data class A(val a: Boolean)
