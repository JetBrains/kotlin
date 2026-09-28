// LANGUAGE: +ExplicitBackingFields

class A {
    val prop: Any
        @Ann field: Int = run {
            fun local() {}
            1
        }
}

val topLevel: Any
    field: Int = 1
    get() = field

annotation class Ann
