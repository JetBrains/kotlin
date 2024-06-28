class A {
    operator fun component1() = 1
    operator fun component2() = ""
}

fun test() {
    val (_, _) = A()
    val (_, _, _) = <!COMPONENT_FUNCTION_MISSING!>A()<!>

    val (_: Int, _: String) = A()
    val (_: String, _) = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>A()<!>

    val f: (A) -> Int = { (_, _) -> 1 }
    val g: (A) -> Int = { <!COMPONENT_FUNCTION_MISSING!>(_, _, _)<!> -> 2 }
    val h: (A) -> Int = { (<!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>_: String<!>, _) -> 3}
}