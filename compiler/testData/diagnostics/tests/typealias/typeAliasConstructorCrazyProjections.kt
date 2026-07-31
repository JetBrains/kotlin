// RUN_PIPELINE_TILL: FRONTEND

class Bound<X, Y : X>(val x: X, val y: Y)
typealias B<X, Y> = Bound<X, Y>
typealias BOutIn<T> = Bound<out List<T>, in T>
typealias BInIn<T> = Bound<in List<T>, in T>

fun <T> listOf(): List<T> = null!!

// Unresolved reference is ok here:
// we can't create a substituted signature for type alias constructor
// since it has 'out' type projection in 'in' position.
val test1 = <!CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION_ERROR!><!INAPPLICABLE_CANDIDATE!>BOutIn<!>(<!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>(), null!!)<!>

val test2 = <!CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION_ERROR!><!INAPPLICABLE_CANDIDATE, UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>BInIn<!>(listOf(), null!!)<!>

/* GENERATED_FIR_TAGS: capturedType, checkNotNullCall, classDeclaration, functionDeclaration, inProjection, nullableType,
outProjection, primaryConstructor, propertyDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeConstraint, typeParameter */
