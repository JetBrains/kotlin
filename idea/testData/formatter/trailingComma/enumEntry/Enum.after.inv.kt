// SET_TRUE: ALLOW_TRAILING_COMMA

enum class Enum1 {
    A, B, ;
}

enum class Enum2 {
    A, B;
}

enum class Enum3 {
    A, B
    ;
}

enum class Enum4 {
    A, B,
}

enum class Enum5 {
    A, B,
}

enum class Enum6(val a: Int) {
    A(
            1
    ),
    B,
}

enum class Enum7(val a: Int) {
    A(
            1
    ),
    B, ;
}

enum class Enum8(val a: Int) {
    A(
            1
    ),
    B;
}