package foo

class A

class B

fun A.x<caret>(): String = "A.x"

val B.x: String
    get() = "B.x"