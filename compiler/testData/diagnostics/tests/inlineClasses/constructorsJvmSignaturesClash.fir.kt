// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class X(val x: Int)
inline class Z(val x: Int)

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