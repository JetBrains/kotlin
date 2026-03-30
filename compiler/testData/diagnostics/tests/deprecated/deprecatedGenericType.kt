// RUN_PIPELINE_TILL: BACKEND
@Deprecated("")
class DeprecatedClassWithParameter<T>

fun foo(
    d1: <!DEPRECATION!>DeprecatedClassWithParameter<!><List<Int>>,
    d2: List<<!DEPRECATION!>DeprecatedClassWithParameter<!><Int>>
) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, stringLiteral, typeParameter */
