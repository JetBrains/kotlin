// FIR_IDENTICAL
// !DIAGNOSTICS: -NOTHING_TO_INLINE
external class A {
    class B
}

inline fun A.foo(x: Int): String = asDynamic().foo(x)

inline operator fun A.get(x: Int): String = asDynamic()[x]

inline operator fun A.B.get(x: Int): String = asDynamic()[x]
