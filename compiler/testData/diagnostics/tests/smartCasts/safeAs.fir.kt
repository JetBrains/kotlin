// See also KT-10992: we should have no errors for all unsafe hashCode() calls

fun foo(arg: Any?) {
    val x = arg as? Any ?: return
    arg.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
    x.hashCode()
}

fun bar(arg: Any?) {
    arg as? Any ?: return
    arg.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
}

fun gav(arg: Any?) {
    arg as? String ?: return
    arg.<!UNRESOLVED_REFERENCE!>length<!>
}
