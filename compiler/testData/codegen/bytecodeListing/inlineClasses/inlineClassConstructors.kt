// !LANGUAGE: +InlineClasses

inline class Foo(val x: Int) {
    constructor(x: Long) : this(x.toInt())
    internal constructor(x: Int, y: Int) : this(x + y)
    private constructor(x: Short) : this(x.toInt())
}