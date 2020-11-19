// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

package kotlin

annotation class JvmInline

@JvmInline
value class X(val x: Int)
@JvmInline
value class Z(val x: Int)

class TestOk1(val a: Int, val b: Int) {
    constructor(x: X) : this(x.x, 1)
}

class TestErr1(val a: Int) {
    constructor(x: X) : this(x.x)
}

class TestErr2(val a: Int, val b: Int) {
    constructor(x: X) : this(x.x, 1)
    constructor(z: Z) : this(z.x, 2)
}