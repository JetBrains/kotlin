fun foo() {}

val x: Unit? = when ("A") {
    "B" -> foo()
}
