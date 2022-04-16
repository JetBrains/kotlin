// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
// See also: KT-11998
data class My(val x: Boolean?)

fun doIt() {}

fun foo(my: My) {
    if (my.x != null) {
        // my.x should be smart-cast
        if (my.x) doIt()
        <!NO_ELSE_IN_WHEN!>when<!> (my.x) {
            true -> doIt()
        }
        when {
            my.x -> doIt()
        }
    }
}

fun bar(x: Boolean?) {
    if (x != null) {
        // x should be smart-cast
        if (x) doIt()
        <!NO_ELSE_IN_WHEN!>when<!> (x) {
            true -> doIt()
        }
        when {
            x -> doIt()
        }
    }
}
