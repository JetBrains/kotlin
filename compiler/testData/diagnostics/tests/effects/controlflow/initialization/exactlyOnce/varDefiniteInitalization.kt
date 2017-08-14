// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun myRun(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: () -> Unit) = block()

fun definiteVarInitialization() {
    var x: Int
    myRun { x = 42 }
    x.inc()
}

fun definiteVarReassignment() {
    var x: Int
    myRun { x = 42 }
    x.inc()
    myRun { x = 43 }
    x.inc()
    x = 44
    x.inc()
}

fun nestedVarInitialization() {
    var x: Int
    myRun { myRun { myRun { x = 42 } } }
    x.inc()
    myRun { myRun { myRun { x = 42 } } }
}


fun notAnExpression() {
    var x: Int = 0
    myRun { if (true) x = 42 }
    x.inc()
}