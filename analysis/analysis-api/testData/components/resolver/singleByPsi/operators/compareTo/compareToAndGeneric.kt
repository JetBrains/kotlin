class MyClass {
    operator fun compareTo(value: MyClass): Int = 0
}

fun usage(m: MyClass, m2: MyClass) {
    m.compare<caret>To(2)
}

operator fun <T> T.compareTo(int: Int): Int = 0