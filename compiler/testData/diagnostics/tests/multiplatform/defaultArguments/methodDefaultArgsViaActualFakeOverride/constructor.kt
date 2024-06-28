// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect class Foo(p: Int = 1)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
open class Base(p1: Int, p2: Int, p3: Int) {
    constructor(p1: Int, p2: Int) : this(p1, p2, 0)
}

actual class Foo actual constructor(p: Int) : Base(p, p)
