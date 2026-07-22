// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// DONT_TARGET_EXACT_BACKEND: NATIVE
//    ^^^ some native tests don't support API_VERSION directive
//    ^^^ fix or wait till version bump

// MODULE: lib
// FILE: lib.kt

open class Base(val n: Int) {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = n == other.n
}

// MODULE: box(lib)
// FILE: box.kt

class Child(n: Int) : Base(n)

class ChildOverriding(n: Int) : Base(n) {
    override fun equals(other: Any?): Boolean = n == other.n
}

fun bar(b: Base, x: Any?): Boolean = b == x

fun box(): String {
    if (Base(1) != Base(1)) return "FAIL 1"
    if (!bar(Base(42), Base(42))) return "FAIL 2"
    if (bar(Base(42), Base(1))) return "FAIL 3"
    if (Child(1) != Base(1)) return "FAIL 4"
    if (ChildOverriding(1) != Base(1)) return "FAIL 5"
    if (ChildOverriding(1) != Child(1)) return "FAIL 6"
    if (ChildOverriding(1) == ChildOverriding(2)) return "FAIL 7"
    return "OK"
}
