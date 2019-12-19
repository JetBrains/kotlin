// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// !LANGUAGE: +NewInference
// ISSUE: KT-32429

import kotlin.properties.Delegates.observable

class Test {
    var test by observable(0) { _, _, _ ->
    }
}

fun box(): String = "OK"
