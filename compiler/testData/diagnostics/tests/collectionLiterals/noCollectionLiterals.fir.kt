// !WITH_NEW_INFERENCE
fun test(): Array<Int> {
    [1, 2]
    [1, 2][0]
    [1, 2].get(0)

    foo([""])

    val p = [1, 2] <!UNRESOLVED_REFERENCE!>+<!> [3, 4]

    return [1, 2]
}

fun foo(a: Array<String> = [""]) {}

class A(val a: Array<Int> = [])
