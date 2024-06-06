class MyClass {
    override fun equals(other: Any?): Boolean {
        return other?.equals(this) ?: false
    }
}

fun check(m: MyClass?, m2: MyClass?) {
    val f = m ?: m2 ?: null
    f ?: m2
    m2 ?: m
}
