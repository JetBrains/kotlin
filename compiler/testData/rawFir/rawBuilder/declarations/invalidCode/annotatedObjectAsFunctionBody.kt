// ISSUE: KT-81687

annotation class Anno

fun foo() =

@Anno
object Annotated { }
