annotation class base

val x: List<@base String>? = null

val y: List<@[base] String>? = null

@Target(AnnotationTarget.TYPE)
annotation class typeAnn

fun foo(list: List<@typeAnn Int>) = list