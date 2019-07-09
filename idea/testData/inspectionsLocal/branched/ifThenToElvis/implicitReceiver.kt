// WITH_RUNTIME
fun String?.foo() = <caret>if (this == null) true else isEmpty()