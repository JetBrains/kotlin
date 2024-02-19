fun Int?.foo() = this?.hashCode() ?: 0

val x = Int?:<caret>:foo