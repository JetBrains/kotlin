class MyClass() {
    constructor(i: Int): this()
}

operator fun <T> T.contains(int: Int): Boolean = true

fun usage() {
    1 in MyClass()
    MyClass().contains(2)
    2 !in MyClass()

    when (1) {
        in MyClass() -> true
    }

    when (val f = 3) {
        !in MyClass() -> false
    }
}
