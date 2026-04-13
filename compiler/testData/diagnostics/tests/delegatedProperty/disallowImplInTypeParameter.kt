// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KProperty0

val a: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> A()

class A {
    fun getValue(t: Any?, p: KProperty0<*>): Int = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, propertyDeclaration,
propertyDelegate, starProjection */
