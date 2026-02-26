// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KProperty

class InnerAndNestedScope {
    val city: List<String>
        field = mutableListOf("1", "2")

    inner class Inner {
        fun foo() {
            val a: KProperty<List<String>> = ::city
            val b: KProperty<MutableList<String>> <!INITIALIZER_TYPE_MISMATCH!>=<!> ::city
            val c: KProperty<List<String>> = InnerAndNestedScope::city
            val d: KProperty<MutableList<String>> <!INITIALIZER_TYPE_MISMATCH!>=<!> InnerAndNestedScope::city
        }
    }

    class Nested {
        fun bar() {
            val a: KProperty<List<String>> = InnerAndNestedScope::city
            val b: KProperty<MutableList<String>> <!INITIALIZER_TYPE_MISMATCH!>=<!> InnerAndNestedScope::city
        }
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, explicitBackingField, functionDeclaration, inner,
localProperty, nestedClass, propertyDeclaration, stringLiteral */
