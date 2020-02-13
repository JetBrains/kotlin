class Rule(val apply:() -> Unit)

fun bar() {}

fun foo() {
    val rule: Rule? = Rule { bar() }

    // this compiles and works
    val apply = rule?.apply
    if (apply != null) <!DEBUG_INFO_SMARTCAST!>apply<!>()

    // this compiles and works
    rule?.apply?.invoke()

    // this should be an error
    rule?.<!UNSAFE_IMPLICIT_INVOKE_CALL!>apply<!>()

    // these both also ok (with smart cast / unnecessary safe call)
    if (rule != null) {
        <!DEBUG_INFO_SMARTCAST!>rule<!>.apply()
        rule<!UNNECESSARY_SAFE_CALL!>?.<!>apply()
    }
}