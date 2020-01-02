// !CHECK_TYPE

data class A(var x: Int, var y: String)

fun foo(a: A) {
    checkSubtype<Int>(a.component1())
    checkSubtype<String>(a.component2())
}
