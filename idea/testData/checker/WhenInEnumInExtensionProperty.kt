// FIR_IDENTICAL
// KT-3750 When without else

enum class A {
    e1,
    e2
}
class B(val a: A)
val B.foo: Int
    get() {
        // Check absence [NO_ELSE_IN_WHEN] error
        return when (a) {
            A.e1 -> 1
            A.e2 -> 3
        }
    }
