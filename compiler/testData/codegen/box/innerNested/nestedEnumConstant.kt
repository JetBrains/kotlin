// IGNORE_BACKEND_FIR: JVM_IR
class Outer {
    enum class Nested {
        O,
        K
    }
}

fun box() = "${Outer.Nested.O}${Outer.Nested.K}"
