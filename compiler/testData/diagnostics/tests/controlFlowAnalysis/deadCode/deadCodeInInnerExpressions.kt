// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testCompound() {
    operator fun Nothing.get(i: Int) {}
    todo()<!UNREACHABLE_CODE!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>[12]<!>
}

fun testCompound1() {
    operator fun Int.times(s: String): Array<String> = throw Exception()
    <!UNREACHABLE_CODE!>(<!>todo() <!UNREACHABLE_CODE!>* "")[1]<!>
}

fun todo(): Nothing = throw Exception()