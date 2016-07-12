fun Int?.foo() = this?.hashCode() ?: 0

val x = { arg: Int? -> arg.foo() <caret>}