// ISSUE: KT-65337

inline fun bar(s: () -> Unit) {
    (<!REDUNDANT_LABEL_WARNING!><!UNDERSCORE_IS_RESERVED!>_<!>@<!> s)()
}
