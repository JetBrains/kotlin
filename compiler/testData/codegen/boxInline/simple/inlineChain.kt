// FILE: k.kt

inline fun f1(): String = "OK"

// FILE: h.kt

inline fun f2(): String = f1()

// FILE: g.kt


inline fun f3(): String = f2()

// FILE: f.kt


inline fun f4(): String = f3()

// FILE: e.kt


inline fun f5(): String = f4()

// FILE: d.kt


inline fun f6(): String = f5()

// FILE: c.kt

inline fun f7(): String = f6()

// FILE: b.kt

inline fun f8(): String = f7()


// FILE: a.kt

fun box(): String {
    return f8()
}