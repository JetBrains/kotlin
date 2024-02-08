// ISSUE: KT-65337

inline fun bar(s: () -> Unit) {
    (<!UNRESOLVED_REFERENCE!><!UNDERSCORE_IS_RESERVED!>_<!>@ s<!>)()
}
