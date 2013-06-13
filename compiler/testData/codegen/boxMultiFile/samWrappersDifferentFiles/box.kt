fun box(): String {
    val class1 = getWrapped1().getClass()
    val class2 = getWrapped2().getClass()

    return if (class1 != class2) "OK" else "Same class: $class1"
}