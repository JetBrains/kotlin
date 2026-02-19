// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: declaration.kt
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

sealed class Status {
    class Ok : Status() {}
    class Error(val message: String) : Status()
}

val Status.isError: Boolean
    get() {
        contract { returns (true) implies (this@isError is Status.Error) }
        return this is Status.Error
    }

context(s: Status)
val isContextError: Boolean
    get() {
        contract { returns (true) implies (s is Status.Error) }
        return s is Status.Error
    }

// FILE: usageSameModule.kt
fun testFromTheSameModule(status: Status) {
    if (status.isError) {
        status.message
    }
}

context(status: Status)
fun testWithContextFromTheSameModule() {
    if (isContextError) {
        status.message
    }
}

// MODULE: main(lib)
// FILE: main.kt
import Status
import isError

fun testFromOtherModule(status: Status) {
    if (status.isError) {
        status.message
    }
}

context(status: Status)
fun testWithContextFromOtherModule() {
    if (isContextError) {
        status.message
    }
}
