// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER, -SENSELESS_COMPARISON, -DEBUG_INFO_SMARTCAST

fun takeNotNull(s: String) {}
fun <T> notNull(): T = TODO()
fun <T> nullable(): T? = null
fun <T> dependOn(x: T) = x

fun test() {
    <!NI;UNREACHABLE_CODE!>takeNotNull(<!><!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>notNull<!>()<!NI;UNREACHABLE_CODE!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)<!>
    <!NI;UNREACHABLE_CODE!>takeNotNull(<!NI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>nullable<!>()!!)<!>

    <!NI;UNREACHABLE_CODE!>var x: String? = null<!>
    <!NI;UNREACHABLE_CODE!>takeNotNull(dependOn(x)!!)<!>
    <!NI;UNREACHABLE_CODE!>takeNotNull(dependOn(dependOn(x))!!)<!>
    <!NI;UNREACHABLE_CODE!>takeNotNull(dependOn(dependOn(x)!!))<!>
    <!NI;UNREACHABLE_CODE!>takeNotNull(dependOn(dependOn(x!!)))<!>

    <!NI;UNREACHABLE_CODE!>if (x != null) {
        takeNotNull(dependOn(x)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
        takeNotNull(dependOn(dependOn(x))<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>)
        takeNotNull(dependOn(dependOn(x)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>))
    }<!>
}