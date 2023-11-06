// FIR_IDENTICAL
// ISSUE: KT-62447, KT-62628
// FIR_DUMP

fun main() {
    val x: <!WRONG_ANNOTATION_TARGET!>@SinceKotlin("2.0")<!><!SYNTAX!><!>
}

@Target(AnnotationTarget.TYPE)
annotation class Anno

val prop: @Anno <!UNRESOLVED_REFERENCE!>Foo<!>? = null
