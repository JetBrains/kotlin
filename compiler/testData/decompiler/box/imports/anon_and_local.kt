// FILE: AnonAndLocalDeclarations.kt
package ru.spbau.mit.declaration

open class FailedOk {
    open fun ok() = "Fail"
}

val fixedOk = object : FailedOk() {
    override fun ok(): String = "OK"
}

// FILE: AnonAndLocalInvocations.kt
package ru.spbau.mit.invocation


import ru.spbau.mit.declaration.FailedOk
import ru.spbau.mit.declaration.fixedOk

fun box(): String {
    val failedOk = FailedOk()
    val localAnonym = object : FailedOk() {
        override fun ok() = "OK"
    }
    return if (localAnonym.ok() == fixedOk.ok()) {
        "OK"
    } else {
        failedOk.ok()
    }
}