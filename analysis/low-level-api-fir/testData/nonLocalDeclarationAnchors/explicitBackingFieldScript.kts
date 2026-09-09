// LANGUAGE: +ExplicitBackingFields

val prop: Any
    field: Int = run {
        fun local() {}
        1
    }
    get() = field

class A {
    val member: Any
        @Ann field: Int = 1
}

annotation class Ann
