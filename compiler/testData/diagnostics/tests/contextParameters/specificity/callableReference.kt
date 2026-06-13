// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments +CallableReferencesToContextual

class A
class B

fun foo0() {}

context(a: A)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo0()<!> {}


context(a: A)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>val foo1: () -> Int<!> get() = { 42 }

val foo1: context(A) () -> Int get() = { 42 }


context(a: A)
val foo2: Int get() = 42

context(b: B)
val foo2: Int get() = 42


context(a: A)
val foo3: Int get() = 42

context(b: B)
val foo3: String get() = "42"

context(a: A)
fun foo4(): Int = 42

context(b: B)
fun foo4(): String = "42"


fun usage() {
    fun <A, R> context(context: A, block: context(A) () -> R): R = block(context)

    foo0()
    val t00 = ::foo0
    val t01: () -> Unit = ::foo0
    val t02: context(A) () -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> ::foo0
    context(A()) {
        val t03 = ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo0<!>
        val t04: () -> Unit = ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo0<!>
        val t05: context(A) () -> Unit = ::<!NONE_APPLICABLE!>foo0<!>
    }

    val t10 = ::foo1
    val t12: context(A) () -> (() -> Int) <!INITIALIZER_TYPE_MISMATCH!>=<!> ::foo1
    val t13: () -> (context(A) () -> Int) = ::foo1
    context(A()) {
        val t14 = ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo1<!>
        val t15: context(A) () -> (() -> Int) = ::<!NONE_APPLICABLE!>foo1<!>
        val t16: () -> (() -> Int) = ::foo1
        val t17: () -> (context(A) () -> Int) = ::foo1
    }

    val t20 = ::<!NONE_APPLICABLE!>foo2<!>
    val t21: () -> Int = ::<!NONE_APPLICABLE!>foo2<!>
    val t22: context(A) () -> Int = ::<!NONE_APPLICABLE!>foo2<!>
    val t23: context(B) () -> Int = ::<!NONE_APPLICABLE!>foo2<!>
    context(A()) {
        val t24 = ::foo2
        val t25: () -> Int = ::foo2
        val t26: context(A) () -> Int <!INITIALIZER_TYPE_MISMATCH!>=<!> ::foo2
        val t27: context(B) () -> Int <!INITIALIZER_TYPE_MISMATCH!>=<!> ::foo2
    }

    val t30 = ::<!NONE_APPLICABLE!>foo3<!>
    val t31: () -> Int = ::<!NONE_APPLICABLE!>foo3<!>
    val t32: () -> String = ::<!NONE_APPLICABLE!>foo3<!>
    val t33: context(A) () -> Int = ::<!NONE_APPLICABLE!>foo3<!>
    val t34: context(B) () -> String = ::<!NONE_APPLICABLE!>foo3<!>
    context(A()) {
        val t35 = ::foo3
        val t36: () -> Int = ::foo3
        val t37: () -> String <!INITIALIZER_TYPE_MISMATCH!>=<!> ::foo3
        val t38: context(A) () -> Int <!INITIALIZER_TYPE_MISMATCH!>=<!> ::foo3
        val t39: context(B) () -> String <!INITIALIZER_TYPE_MISMATCH!>=<!> ::foo3
    }

    val t40 = ::<!NONE_APPLICABLE!>foo4<!>
    val t41: () -> Int = ::<!NONE_APPLICABLE!>foo4<!>
    val t42: () -> String = ::<!NONE_APPLICABLE!>foo4<!>
    val t43: context(A) () -> Int = ::<!NONE_APPLICABLE!>foo4<!>
    val t44: context(B) () -> String = ::<!NONE_APPLICABLE!>foo4<!>
    context(A()) {
        val t45 = ::foo4
        val t46: () -> Int = ::foo4
        val t47: () -> String <!INITIALIZER_TYPE_MISMATCH!>=<!> ::foo4
        val t48: context(A) () -> Int <!INITIALIZER_TYPE_MISMATCH!>=<!> ::foo4
        val t49: context(B) () -> String <!INITIALIZER_TYPE_MISMATCH!>=<!> ::foo4
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext */
