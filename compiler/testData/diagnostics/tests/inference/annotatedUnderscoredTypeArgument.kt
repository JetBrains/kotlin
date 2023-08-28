// FIR_IDENTICAL
// !LANGUAGE: +PartiallySpecifiedTypeArguments
// !DIAGNOSTICS: -UNCHECKED_CAST
// WITH_STDLIB

fun <K, T> foo(x: (K) -> T): Pair<K, T> = (1 as K) to (1f as T)

@Repeatable
@Target(AnnotationTarget.TYPE)
annotation class Anno

@Repeatable
@Target(AnnotationTarget.TYPE)
annotation class Anno2

@Repeatable
@Target(AnnotationTarget.TYPE)
annotation class Anno3(val x: String)

fun box(): String {
    val x = foo<@Anno Int, <!UNSUPPORTED("annotations on an underscored type argument")!>@Anno<!> _> { it.toFloat() }
    val y: Pair<Int, Float> = foo<@[<!UNSUPPORTED!>Anno<!> <!UNSUPPORTED!>Anno2<!>] _, <!UNSUPPORTED!>@Anno<!> _> { it.toFloat() }
    val z1: Pair<Int, Float> = foo<<!UNSUPPORTED!>@Anno<!> <!UNSUPPORTED!>@Anno2<!> /**/ _, @[/**/ <!UNSUPPORTED!>Anno<!>    /**/ ] _> { it.toFloat() }
    val z2: Pair<Int, Float> = foo<<!UNSUPPORTED!>@Anno3("")<!> /**/ _, @[/**/ <!UNSUPPORTED!>Anno<!>    /**/ <!UNSUPPORTED!>Anno3("")<!> /**/] _,> { it.toFloat() }

    val z31: Pair<@Anno3("") <!UNRESOLVED_REFERENCE!>_<!>, Float> = 1 to 1f
    val z33: Pair<@Anno3("") (<!UNRESOLVED_REFERENCE!>_<!>), Float> = 1 to 1f
    val z35: Pair<(@Anno3("") (<!UNRESOLVED_REFERENCE!>_<!>)), Float> = 1 to 1f

    return "OK"
}
