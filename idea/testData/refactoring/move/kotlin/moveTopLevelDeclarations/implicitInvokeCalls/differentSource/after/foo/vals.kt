package foo

import bar.CrExtended

val valWithFunType = fun (): Unit {}
val valWithExtFunType = fun CrExtended.(): Unit {}
val Int.extValWithFunType get() = fun (): Unit {}
val Int.extValWithExtFunType get() = fun CrExtended.(): Unit {}

