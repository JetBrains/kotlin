// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters

@file:OptIn(ExperimentalContracts::class)

import kotlin.contracts.*

class Foo {
    var Int?.prop: Int?
        get() {
            contract { returns() implies (this@prop != null) }
            return null
        }
        set(v: Int?) {
            contract {
                returns() implies (v != null)
                returns() implies (this@prop != null)
            }
        }

    sealed class Status {
        class Ok : Status() {}
        class Error(val message: String) : Status()
    }

    val Status.isError: Boolean
        get() {
            contract { returns(true) implies (this@isError is Status.Error) }
            return this is Status.Error
        }

    context(s: Status)
    val isContextError: Boolean
        get() {
            contract { returns(true) implies (s is Status.Error) }
            return s is Status.Error
        }
}
