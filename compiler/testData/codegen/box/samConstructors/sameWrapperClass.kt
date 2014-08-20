fun box(): String {
    val f = { }
    val class1 = (Runnable(f) as Object).getClass()
    val class2 = (Runnable(f) as Object).getClass()

    return if (class1 == class2) "OK" else "$class1 $class2"
}
