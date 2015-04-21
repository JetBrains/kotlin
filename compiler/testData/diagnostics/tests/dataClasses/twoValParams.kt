// !CHECK_TYPE

data class A(val x: Int, val y: String)

fun foo(a: A) {
    checkSubtype<Int>(a.component1())
    checkSubtype<String>(a.component2())
}
