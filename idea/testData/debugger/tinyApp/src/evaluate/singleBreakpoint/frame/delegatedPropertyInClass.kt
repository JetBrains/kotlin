package delegatedPropertyInClass

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

fun main(args: Array<String>) {
    val a = A()
    //Breakpoint!
    args.size
}

class A {
    val prop by MyDelegate()
    val propEx by MyDelegateThrowsException()
}

class MyDelegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

class MyDelegateThrowsException {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = throw IllegalStateException()
}

// PRINT_FRAME
// SKIP: suppressedExceptions
// SKIP: stackTrace
