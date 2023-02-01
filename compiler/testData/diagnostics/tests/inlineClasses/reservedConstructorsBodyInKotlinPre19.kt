// !LANGUAGE: -ValueClassesSecondaryConstructorWithBody
// WITH_STDLIB

@JvmInline
value class Foo(val x: String) {
    constructor(i: Int) : this(i.toString()) <!UNSUPPORTED_FEATURE!>{<!>
        println(i)
    }
}
