// KT-15245 Report deprecation on associated declarations if level is greater than the deprecation on the declaration itself

@Deprecated("error", level = DeprecationLevel.ERROR)
class Foo @Deprecated("warning", level = DeprecationLevel.WARNING) constructor()

fun test1() = <!DEPRECATION_ERROR!>Foo<!>()

fun test2(): <!DEPRECATION_ERROR!>Foo<!> = <!DEPRECATION_ERROR!>Foo<!>()
