// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KProperty

class CallableReference {
    val a: Any
        field: String = ""

    fun foo() {
        val x: KProperty<String> <!INITIALIZER_TYPE_MISMATCH!>=<!> ::a
        val y: KProperty<Any> = ::a
    }
}

fun outsideOfScope() {
    val x: KProperty<String> <!INITIALIZER_TYPE_MISMATCH!>=<!> CallableReference::a
    val y: KProperty<Any> = CallableReference::a
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, explicitBackingField, functionDeclaration, localProperty,
propertyDeclaration, stringLiteral */
