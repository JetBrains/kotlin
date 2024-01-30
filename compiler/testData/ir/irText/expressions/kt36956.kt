// FIR_IDENTICAL
class A<T>(private val value: T) {
    operator fun get(i: Int) = value
    operator fun set(i: Int, v: T) {}
}

val aFloat = A<Float>(0.0f)

val aInt = (aFloat[1])--
