// "Safe delete 'WORLD'" "true"
enum class MyEnum(val i: Int) {
    HELLO(42),
    WORLD<caret>("42"),
    E(24)
    ;

    constructor(s: String): this(42)
}

fun test() {
    MyEnum.HELLO
}