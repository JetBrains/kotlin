// LANGUAGE: +ExplicitBackingFields

class A {
    val x: Number
        field: Int = run {
            fun doSmth(i: String) = 4
            <expr>doSmth</expr>("str")
        }
}
