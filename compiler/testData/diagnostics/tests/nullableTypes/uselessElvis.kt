// !DIAGNOSTICS: -UNUSED_PARAMETER, -SENSELESS_COMPARISON, -DEBUG_INFO_SMARTCAST

fun <T: Any?> test1(t: Any?): Any {
    return t <!UNCHECKED_CAST!>as T<!> ?: ""
}

fun <T: Any> test2(t: Any?): Any {
    return t <!UNCHECKED_CAST!>as T<!> <!USELESS_ELVIS!>?: ""<!>
}

fun <T: Any?> test3(t: Any?): Any {
    if (t != null) {
        return t <!USELESS_ELVIS!>?: ""<!>
    }

    return 1
}

fun takeNotNull(s: String) {}
fun <T> notNull(): T = TODO()
fun <T> nullable(): T? = null
fun <T> dependOn(x: T) = x

fun test() {
    <!UNREACHABLE_CODE!>takeNotNull(<!><!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>notNull<!>() <!UNREACHABLE_CODE!><!USELESS_ELVIS!>?: ""<!>)<!>
    <!UNREACHABLE_CODE!>takeNotNull(<!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>nullable<!>() ?: "")<!>

    <!UNREACHABLE_CODE!>val x: String? = null<!>
    <!UNREACHABLE_CODE!>takeNotNull(dependOn(x) ?: "")<!>
    <!UNREACHABLE_CODE!>takeNotNull(dependOn(dependOn(x)) ?: "")<!>
    <!UNREACHABLE_CODE!>takeNotNull(dependOn(dependOn(x as String)) <!USELESS_ELVIS!>?: ""<!>)<!>

    <!UNREACHABLE_CODE!>if (x != null) {
        takeNotNull(dependOn(x) <!USELESS_ELVIS!>?: ""<!>)
        takeNotNull(dependOn(dependOn(x)) <!USELESS_ELVIS!>?: ""<!>)
        takeNotNull(dependOn(dependOn(x) as? String) ?: "")
    }<!>

    <!UNREACHABLE_CODE!>takeNotNull(bar()!!)<!>
}

inline fun <reified T : Any> reifiedNull(): T? = null

fun testFrom13648() {
    takeNotNull(reifiedNull() ?: "")
}

fun bar() = <!UNRESOLVED_REFERENCE!>unresolved<!>