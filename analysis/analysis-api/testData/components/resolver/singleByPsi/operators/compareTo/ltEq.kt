class MyClass {
    operator fun compareTo(value: MyClass): Int = 0
}

fun usage(m: MyClass, m2: MyClass) {
    m <<caret>= m2
}