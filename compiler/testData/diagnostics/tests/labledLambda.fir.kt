// ISSUE: KT-65337

inline fun bar(s: () -> Unit) {
    (<!UNDERSCORE_IS_RESERVED!>_<!>@ s)()
}
