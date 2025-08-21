const val PARAMETER_NAME: String = "myName"

fun foo(bl<caret>ock: (@ParameterName(PARAMETER_NAME) Int) -> Unit) {}