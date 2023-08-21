// MODULE: lib
// FILE: 1.kt

inline fun callsFunThatIsNeverDirectlyReferenced(): Any = dontCallMeDirectly()

fun dontCallMeDirectly(): Any = "OK"

// MODULE: main(lib)
// FILE: 2.kt

fun box(): String = callsFunThatIsNeverDirectlyReferenced().toString()
