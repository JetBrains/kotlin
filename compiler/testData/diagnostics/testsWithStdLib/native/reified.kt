import kotlin.jvm.*

native fun <<!REIFIED_TYPE_PARAMETER_NO_INLINE!>reified<!> T> foo()
<!NATIVE_DECLARATION_CANNOT_BE_INLINED!>inline native fun <reified T> bar()<!>