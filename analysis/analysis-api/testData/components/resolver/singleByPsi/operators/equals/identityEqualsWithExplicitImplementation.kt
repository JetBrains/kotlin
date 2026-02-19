class MyClass {
    override fun equals(other: Any?): Boolean = true
}

fun myClass(m1: MyClass, m2: MyClass) {
    m1 =<caret>== m2
}
