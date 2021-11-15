// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: overload-resolution, receivers -> paragraph 6 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: The phantom static implicit this receiver has higher priority than the current class companion object receiver;
 */


var flag = false
fun box(): String {
    Case1.A1.foo()
    if (!flag)
        return "OK"
    return "NOK"
}

enum class Case1 {
    A1, A2;

    companion object values {
        operator fun invoke() {
            flag = true
        }
    }

    fun foo() {
        values()
    }
}
