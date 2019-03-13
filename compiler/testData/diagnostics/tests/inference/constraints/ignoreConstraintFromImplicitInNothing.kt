// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

class Foo<T>

fun <T> foo1(f: (T) -> Unit): Foo<T> = Foo()
inline fun <reified T> foo2(f: (T) -> Unit): Foo<T> = Foo()

fun test1() {
    val f1: Foo<out Int> = foo1 { it checkType { _<Int>() } }
    val f2: Foo<in Nothing> = <!OI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>foo1<!> { it <!UNREACHABLE_CODE!><!OI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>checkType<!> { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Nothing>() }<!> }

    val f3: Foo<out Int> = foo2 { it checkType { _<Int>() } }
    val f4: Foo<in Nothing> = <!OI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER, REIFIED_TYPE_FORBIDDEN_SUBSTITUTION!>foo2<!> { it <!UNREACHABLE_CODE!><!OI;IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>checkType<!> { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Nothing>() }<!> }
}