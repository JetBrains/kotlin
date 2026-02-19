// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -REDUNDANT_PROJECTION -CONFLICTING_PROJECTION

interface G

val <T> T.a: Int
    get() = 3

val <T1, T2> Map<T1, T2>.b: String
    get() = "asds"

val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>T : G<!>> G.c: Int get() = 5

val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>T1<!>, T2, T3> List<Map<T2, T3>>.d: Int get() = 6

val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>T: Any<!>> G.e: T?
    get() = null

val <T> List<Map<Int, Map<String, T>>>.f: Int get() = 7

val <T> List<Map<Int, Map<String, out T>>>.g: Int get() = 7
val <T> List<Map<Int, Map<String, in T>>>.h: Int get() = 7

val <T> List<Map<T, Map<T, T>>>.i: Int get() = 7

var <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>T1<!>, <!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>T2<!>, <!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>T3<!>, <!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>T4<!>> p = 1

class C<T1, T2> {
    val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>E<!>> T1.a: Int get() = 3
    val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>E<!>> T2.b: Int get() = 3
    val <E> E.c: Int get() = 3
    val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>E<!>> Map<T1, T2>.d: Int get() = 3
    val <E> Map<T1, E>.e: Int get() = 3
}

val <T : Enum<T>> T.z1: Int
    get() = 4

interface D<T : Enum<T>>

val <X: D<*>> X.z2: Int
    get() = 4

val <<!INCORRECT_TYPE_PARAMETER_OF_PROPERTY!>Y<!>> D<*>.z3: Int
    get() = 4

/* GENERATED_FIR_TAGS: classDeclaration, getter, inProjection, integerLiteral, interfaceDeclaration, nullableType,
outProjection, propertyDeclaration, propertyWithExtensionReceiver, starProjection, stringLiteral, typeConstraint,
typeParameter */
