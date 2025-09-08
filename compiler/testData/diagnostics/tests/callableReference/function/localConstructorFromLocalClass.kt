// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
import kotlin.reflect.KFunction0

fun main() {
    class A
    
    class B {
        val x = ::A
        val f: KFunction0<A> = x
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, localClass, propertyDeclaration */
