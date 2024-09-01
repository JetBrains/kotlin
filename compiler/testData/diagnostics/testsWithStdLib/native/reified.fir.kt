import kotlin.jvm.*

external fun <<!REIFIED_TYPE_PARAMETER_NO_INLINE!>reified<!> T> foo()
inline <!EXTERNAL_DECLARATION_CANNOT_BE_INLINED!>external<!> fun <reified T> bar()
