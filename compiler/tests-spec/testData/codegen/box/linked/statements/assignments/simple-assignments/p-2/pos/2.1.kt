// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * MAIN LINK: statements, assignments, simple-assignments -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: If a property has a setter, it is called using the right-hand side expression as its argument;
 */


val valToSet = 5

class C() {
    var counter = 0
}

fun box(): String {
    val c = C()
    c.counter = valToSet
    if (c.counter == valToSet) return "OK"
    return "NOK"
}
