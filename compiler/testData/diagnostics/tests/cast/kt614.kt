// RUN_PIPELINE_TILL: BACKEND
fun f(a: Collection<*>) = a is List<*><!USELESS_NULLABLE_CHECK!>?<!>