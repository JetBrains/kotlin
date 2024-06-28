// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtNameReferenceExpression
fun <@A T> <expr>T</expr>.test() {}

@Target(AnnotationTarget.TYPE_PARAMETER) annotation class A
