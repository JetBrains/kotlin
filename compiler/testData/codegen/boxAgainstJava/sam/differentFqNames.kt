fun box(): String {
    val f = { }
    val class1 = Runnable(f).javaClass
    val class2 = Custom.Runnable(f).javaClass

    return if (class1 != class2) "OK" else "Same class: $class1"
}