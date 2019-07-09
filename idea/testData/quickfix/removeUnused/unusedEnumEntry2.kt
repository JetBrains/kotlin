// "Safe delete 'WORLD'" "true"
enum class MyEnum(val i: Int) {
    WORLD<caret>("42"),
    HELLO(42)
    ;

    constructor(s: String): this(42)
}

fun test() {
    MyEnum.HELLO
}