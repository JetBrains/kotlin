package test

fun useMethodWithWildcard() = 
    MethodWithWildcard<CharSequence>().method(emptyList<String>())
