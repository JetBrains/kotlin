package stepIntoSpecificKotlinClasses

import stepInto.MyJavaClass

fun main(args: Array<String>) {
    val myClass = MyJavaClass()
    //Breakpoint!
    val a: String = myClass.testNotNullFun()
    val b = 1
}

// REPEAT: 5
// DISABLE_KOTLIN_INTERNAL_CLASSES: false
// TRACING_FILTERS_ENABLED: false
