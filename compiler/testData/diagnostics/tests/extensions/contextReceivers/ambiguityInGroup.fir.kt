// !LANGUAGE: +ContextReceivers

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
    <!UNRESOLVED_REFERENCE!>supertypeMember<!>()
    <!UNRESOLVED_REFERENCE!>member<!>()
    <!UNRESOLVED_REFERENCE!>supertypeExtension<!>()
    <!UNRESOLVED_REFERENCE!>supertypeExtensionGeneric<!>()
    supertypeContextual()
}