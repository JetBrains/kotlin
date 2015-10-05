package delegatedPropertyInClassWoRenderer

import kotlin.properties.Delegates

fun main(args: Array<String>) {
    val a = A()
    //Breakpoint!
    args.size()
}

class A {
    val prop by MyDelegate()
}

class MyDelegate {
    fun getValue(t: Any?, p: PropertyMetadata): Int = 1
}

// RENDER_DELEGATED_PROPERTIES: false
// PRINT_FRAME