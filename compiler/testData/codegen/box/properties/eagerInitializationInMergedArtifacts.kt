// TARGET_BACKEND: JS_IR, JS_IR_ES6
// ES_MODULES

// MODULE: lib

// FILE: eager.kt
@file:JsFileName("clash")

package p

@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
val eagerTrigger = run { observable = "ran"; 1 }

// FILE: inert.kt
@file:JsFileName("clash")

package p

val inertValue = 41

fun inertFun(): Int = inertValue + 1

// FILE: observable.kt
package p

var observable = "not-run"

// MODULE: main(lib)

// FILE: main.kt
import p.observable

fun box(): String =
    if (observable == "ran") "OK"
    else "fail: eager initializer of a merged artifact did not run, observable = $observable"
