package delegatedPropertyInClass

import kotlin.properties.Delegates

fun main(args: Array<String>) {
    val a = A()
    //Breakpoint!
    args.size()
}

class A {
    val prop by MyDelegate()
    val propEx by MyDelegateThrowsException()
}

class MyDelegate {
    fun get(t: Any?, p: PropertyMetadata): Int = 1
}

class MyDelegateThrowsException {
    fun get(t: Any?, p: PropertyMetadata): Int = throw IllegalStateException()
}

// PRINT_FRAME