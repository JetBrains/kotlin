package test

class A(val z: Int) {
    fun calc() = z
}

inline fun call(p: A, s: A.() -> Int): Int {
    return p.s()
}