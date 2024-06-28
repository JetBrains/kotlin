// LAMBDAS: CLASS
// OPT_IN: kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.reflect

class C {
    val o = { O: String -> }
    val k = { K: String -> }

    constructor(y: Int)
    constructor(y: String)
}

fun box(): String =
    (C(0).o.reflect()?.parameters?.singleOrNull()?.name ?: "null") +
            (C("").k.reflect()?.parameters?.singleOrNull()?.name ?: "null")
