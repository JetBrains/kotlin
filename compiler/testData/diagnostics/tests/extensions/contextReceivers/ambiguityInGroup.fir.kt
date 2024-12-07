// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

interface Common {
    fun supertypeMember() {}
}
interface C1 : Common {
    fun member() {}
}
interface C2 : Common {
    fun member() {}
}

fun Common.supertypeExtension() {}

fun <T : Common> T.supertypeExtensionGeneric() {}

context(Common)
fun supertypeContextual() {}

context(C1, C2)
fun test() {
    supertypeMember()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>member<!>()
    <!AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER!>supertypeExtension<!>()
    <!AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER, CANNOT_INFER_PARAMETER_TYPE!>supertypeExtensionGeneric<!>()
    <!AMBIGUOUS_CONTEXT_ARGUMENT!>supertypeContextual<!>()
}
