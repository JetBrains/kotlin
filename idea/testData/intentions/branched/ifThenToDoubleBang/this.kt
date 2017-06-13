// WITH_RUNTIME
fun String?.foo() = if (this == <caret>null) throw NullPointerException() else this