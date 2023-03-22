// IGNORE_REVERSED_RESOLVE
@Repeatable
@Target( AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParameterAnn(val name: String)

interface Generic<Z>

fun <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>@TypeParameterAnn("T") T: Any<!>> whereClauseWithAnnotation() where <!ANNOTATION_IN_WHERE_CLAUSE_WARNING!>@<!DEBUG_INFO_MISSING_UNRESOLVED!>TypeParameterAnn<!>("Prohibit me!!!")<!>  T : Generic<String> {
}

class Foo<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>@TypeParameterAnn("T") T: Any<!>> () where <!ANNOTATION_IN_WHERE_CLAUSE_WARNING!>@<!DEBUG_INFO_MISSING_UNRESOLVED!>TypeParameterAnn<!>("Prohibit me!!!")<!>  T : Generic<String> {
}
