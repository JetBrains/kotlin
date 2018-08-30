// !LANGUAGE: +InlineClasses

inline class Foo(val x: Int) {
    constructor(x: Long) : this(x.toInt())
}