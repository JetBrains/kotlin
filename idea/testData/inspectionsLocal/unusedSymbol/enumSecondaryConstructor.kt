// PROBLEM: none
enum class MyEnum(val i: Int) {
    HELLO(42),
    WORLD("42")
    ;

    constructor<caret>(s: String): this(42)
}

fun test() {
    MyEnum.HELLO
}