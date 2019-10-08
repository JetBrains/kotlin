// FILE: stepIntoMultiFileFacade.kt
package stepIntoMultiFileFacade

fun main(args: Array<String>) {
    //Breakpoint!
    customLib.oneFunSameClassName.oneFunSameFileNameFun()
}

// STEP_INTO: 2
// TRACING_FILTERS_ENABLED: false

// FILE: oneFunSameClassName/1/a1.kt
@file:JvmName("SameNameOneFunSameFileName")
@file:JvmMultifileClass
package customLib.oneFunSameClassName

public fun oneFunSameFileNameFun(): Int {
    return 1
}

// FILE: oneFunSameClassName/2/a2.kt
@file:JvmName("SameNameOneFunSameFileName")
@file:JvmMultifileClass
package customLib.oneFunSameClassName

public fun oneFunSameFileNameFun2(): Int {
    return 1
}