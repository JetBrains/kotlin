annotation class A
annotation class A1(val x: Int)

annotation class B(
        val a: A = A(),
        val x: Int = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>A1(42).x<!>,
        val aa: Array<A> = arrayOf(A())
)