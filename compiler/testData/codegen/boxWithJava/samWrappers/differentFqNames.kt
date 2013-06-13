fun box(): String {
    val f = { }
    val class1 = Runnable(f).getClass()
    val class2 = Custom.Runnable(f).getClass()

    return if (class1 != class2) "OK" else "Same class: $class1"
}