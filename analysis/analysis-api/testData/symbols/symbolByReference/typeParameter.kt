
@Target(AnnotationTarget.TYPE_PARAMETER) annotation class A

fun <@A T> <caret>T.test() {}