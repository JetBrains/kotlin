// FIR_IDENTICAL
// !LANGUAGE: +DataObjects
// TARGET_BACKEND: JVM_IR

import java.io.Serializable

data <!CONFLICTING_JVM_DECLARATIONS!>object A<!> : Serializable {
    <!CONFLICTING_JVM_DECLARATIONS!>private fun readResolve(): Any<!> = this
}

data object B : Serializable {
    private fun readResolve(): B = this
}

data object C {
    private fun readResolve(): Any = this
}
