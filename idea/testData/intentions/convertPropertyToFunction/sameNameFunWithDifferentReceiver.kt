package foo

class A

class B

fun A.x(): String = "A.x"

val B.x<caret>: String
    get() = "B.x"