// !RENDER_DIAGNOSTICS_MESSAGES

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class Ann(val s: String = "")

@Ann("s")
fun foo() {}

val bar = <!INAPPLICABLE_CANDIDATE!>foo<!>(
    15
)