class A<T : Function1<*, Any>>(var x: T) {
    val y = A(<!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH!>fun(x: Any): Any = 1<!>)
}