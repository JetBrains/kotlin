// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testCompound() {
    fun Nothing.get(i: Int) {}
    todo()<!UNREACHABLE_CODE!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>[12]<!>
}

fun testCompound1() {
    fun Int.times(s: String): Array<String> = throw Exception()
    <!UNREACHABLE_CODE!>(<!>todo() <!UNREACHABLE_CODE!>* "")[1]<!>
}

fun todo() = throw Exception()