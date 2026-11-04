// RUN_PIPELINE_TILL: FRONTEND
// COMPARE_WITH_LIGHT_TREE
@Repeatable
@Target( AnnotationTarget.TYPE_PARAMETER)
annotation class TypeParameterAnn(val name: String)

interface Generic<Z>

fun <<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>@TypeParameterAnn("T") T: Any<!>> whereClauseWithAnnotation() where <!ANNOTATION_IN_WHERE_CLAUSE_ERROR!>@TypeParameterAnn("Prohibit me!!!")<!>  T : Generic<String> {
}

class Foo<<!MISPLACED_TYPE_PARAMETER_CONSTRAINTS!>@TypeParameterAnn("T") T: Any<!>> () where <!ANNOTATION_IN_WHERE_CLAUSE_ERROR!>@TypeParameterAnn("Prohibit me!!!")<!>  T : Generic<String> {
}

fun <T> undeclaredTypeParameter() where <!ANNOTATION_IN_WHERE_CLAUSE_ERROR!>@TypeParameterAnn("Prohibit me!!!")<!> <!NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER!>AA<!> : Generic<String> {
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, interfaceDeclaration, nullableType,
primaryConstructor, propertyDeclaration, stringLiteral, typeConstraint, typeParameter */
