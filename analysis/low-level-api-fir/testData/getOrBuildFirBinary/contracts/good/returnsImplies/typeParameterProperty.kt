// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
// LANGUAGE: +AllowContractsOnPropertyAccessors, +ContextParameters
// COMPILATION_ERRORS
// ISSUE: KT-82197
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
context(p: Any)
inline var <reified T> T?.isNotNull: Boolean
    get() {
        contract {
            returns(true) implies (p is T)
        }

        return this != null
    }
    set(value) {
        contract {
            returns() implies (p is T)
        }
    }
