// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FILE: 1/wrapped.kt

fun getWrapped1(): Runnable {
    val f = { }
    return Runnable(f)
}

// FILE: 2/wrapped2.kt

fun getWrapped2(): Runnable {
    val f = { }
    return Runnable(f)
}

// FILE: box.kt

fun box(): String {
    val class1 = getWrapped1().javaClass
    val class2 = getWrapped2().javaClass

    return if (class1 != class2) "OK" else "Same class: $class1"
}
