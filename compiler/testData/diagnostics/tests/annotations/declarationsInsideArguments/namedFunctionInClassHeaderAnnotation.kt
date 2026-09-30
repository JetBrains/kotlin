// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS
// ISSUE: KT-77041
// IGNORE_PHASE_VERIFICATION: invalid code inside annotations

@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
annotation class Anno(val i: Int)

class TypeParameter<@Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1) T>

class SuperType : @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1) Any()

class Bound<T : @Anno(i = fun <!ANONYMOUS_FUNCTION_WITH_NAME!>foo<!>() = 1) Any>

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, localFunction,
nullableType, primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
