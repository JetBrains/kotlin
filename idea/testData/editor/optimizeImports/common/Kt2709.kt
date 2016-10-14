import MyClass.Companion.TEST

fun main() {
    TEST
}

class MyClass {
    companion object {
        object TEST {}
    }
}
