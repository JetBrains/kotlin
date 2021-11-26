// !LANGUAGE: +NewInference
// WITH_STDLIB

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-220
 * MAIN LINK: expressions, indexing-expressions -> paragraph 3 -> sentence 1
 * PRIMARY LINKS: expressions, indexing-expressions -> paragraph 1 -> sentence 1
 * expressions, indexing-expressions -> paragraph 2 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: A[I_0,I_1,...,I_N] is exactly the same as A.get(I_0,I_1,...,I_N), where get is a valid operator function available in the current scope
 */


class A1(val a: Int = 0) {
    operator fun get(x: Any): Any {
        return x
    }
}

class B() {
    operator fun get(x: Any, y: Any, z: Any): Any {
        return x
    }
}

class C() {
    operator fun get(function: () -> Any): Any {
        return function()
    }
}

fun box() : String{
    val a = A1(4444)
    val c  = a [C()[{ 1 + 900 }]]
    val x = a[a[a[a[B()[A1(100500)[ C()[{ 1 + 900 }]  ], 'c', false]]]]]

    if (c == 901 && x == 901)
        return "OK"
    return "NOK"
}