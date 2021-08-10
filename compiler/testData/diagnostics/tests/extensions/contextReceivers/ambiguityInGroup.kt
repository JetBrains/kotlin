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
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>supertypeMember<!>()
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>member<!>()
    <!AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER!>supertypeExtension()<!>
    <!AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>supertypeExtensionGeneric<!>()<!>
    <!MULTIPLE_ARGUMENTS_APPLICABLE_FOR_CONTEXT_RECEIVER!>supertypeContextual()<!>
}