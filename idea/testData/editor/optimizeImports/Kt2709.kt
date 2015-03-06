import MyClass.Default.TEST

fun main() {
    TEST
}

class MyClass {
    default object {
        object TEST {}
    }
}
