// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class Anno(val position: String)

@Anno("str")
context(<expr>@Anno("param")</expr> parameter1 : @Anno("1" + "2") Unresolved, parameter2: List<@Anno("str") Unresolved>)
