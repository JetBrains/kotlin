// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

@Target(AnnotationTarget.TYPE_PARAMETER) annotation class A

fun <@A T> <caret>T.test() {}