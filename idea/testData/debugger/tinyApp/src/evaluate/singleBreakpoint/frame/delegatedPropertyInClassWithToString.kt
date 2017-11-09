package delegatedPropertyInClassWithToString

import kotlin.reflect.KProperty

fun main(args: Array<String>) {
    val a = A()
    //Breakpoint!
    args.size
}

class A {
    val prop by MyDelegate()

    override fun toString(): String = "KotlinTest"
}

class MyDelegate {
    operator fun getValue(t: Any?, p: KProperty<*>): Int = 1
}

// PRINT_FRAME
