package test

class A(val z: Int) {
    fun calc() = z
}

inline fun call(p: Int, s: (Int) -> A): Int {
    return s(p).z
}