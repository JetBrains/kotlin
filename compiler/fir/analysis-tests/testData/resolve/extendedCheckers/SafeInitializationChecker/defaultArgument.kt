class A {
    val def = Def("Hello")
}

data class Def(val a: String = run { y },  <!ACCESS_TO_UNINITIALIZED_VALUE!>val y: String = "Tmp"<!>)