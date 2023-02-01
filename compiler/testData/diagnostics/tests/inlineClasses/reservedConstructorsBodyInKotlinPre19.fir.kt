// !LANGUAGE: -ValueClassesSecondaryConstructorWithBody
// WITH_STDLIB

@JvmInline
value class Foo(val x: String) {
    constructor(i: Int) : this(i.toString()) <!SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_VALUE_CLASS!>{
        println(i)
    }<!>
}
