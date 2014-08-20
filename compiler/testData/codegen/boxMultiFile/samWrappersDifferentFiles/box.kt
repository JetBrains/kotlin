fun box(): String {
    val class1 = getWrapped1().javaClass
    val class2 = getWrapped2().javaClass

    return if (class1 != class2) "OK" else "Same class: $class1"
}
