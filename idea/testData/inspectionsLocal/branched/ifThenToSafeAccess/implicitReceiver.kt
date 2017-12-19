// WITH_RUNTIME
fun String?.foo() = <caret>if (this == null) null else isEmpty()
