package delegatedPropertyInClassWoRenderer

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

fun main(args: Array<String>) {
    val a = A()
    //Breakpoint!
    args.size
}

class A {
    val prop by MyDelegate()
}

class MyDelegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

// RENDER_DELEGATED_PROPERTIES: false
// PRINT_FRAME
