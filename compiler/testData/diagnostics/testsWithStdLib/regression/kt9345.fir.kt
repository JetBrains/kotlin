//KT-9345 Type inference failure

fun Class<*>.foo(): Any? = kotlin.<!UNRESOLVED_REFERENCE!>objectInstance<!>