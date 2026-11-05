// RUN_PIPELINE_TILL: CODEGEN

@file:Suppress("INLINE_CLASS_DEPRECATED", <!ERROR_SUPPRESSION!>"INLINE_CLASS_DEPRECATED_ERROR"<!>)

inline class X(val x: Int)
inline class Z(val x: Int)
inline value class Str(val str: String)
inline value class Name(val name: String)
inline class NStr(val str: String?)

fun testSimple(x: X) {}
fun testSimple(z: Z) {}

fun testMixed(x: Int, y: Int) {}
fun testMixed(x: X, y: Int) {}
fun testMixed(x: Int, y: X) {}
fun testMixed(x: X, y: X) {}

fun testNewType(s: Str) {}
fun testNewType(name: Name) {}

fun testNullableVsNonNull1(s: Str) {}
fun testNullableVsNonNull1(s: Str?) {}

fun testNullableVsNonNull2(ns: NStr) {}
fun testNullableVsNonNull2(ns: NStr?) {}

<!CONFLICTING_JVM_DECLARATIONS!>fun testFunVsExt(x: X)<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun X.testFunVsExt()<!> {}

<!CONFLICTING_JVM_DECLARATIONS!>fun testNonGenericVsGeneric(x: X, y: Number)<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>fun <T : Number> testNonGenericVsGeneric(x: X, y: T)<!> {}

class C<TC : Number> {
    <!CONFLICTING_JVM_DECLARATIONS!>fun testNonGenericVsGeneric(x: X, y: Number)<!> {}
    <!CONFLICTING_JVM_DECLARATIONS!>fun <T : Number> testNonGenericVsGeneric(x: X, y: T)<!> {}
    <!CONFLICTING_JVM_DECLARATIONS!>fun testNonGenericVsGeneric(x: X, y: TC)<!> {}
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, nullableType, primaryConstructor,
propertyDeclaration, typeConstraint, typeParameter */
