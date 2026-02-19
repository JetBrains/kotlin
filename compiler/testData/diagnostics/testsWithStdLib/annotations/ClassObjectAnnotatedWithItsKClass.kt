// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package test
import kotlin.reflect.KClass

annotation class AnnClass(val a: KClass<*>)

class MyClass {

    @AnnClass(MyClass::class)
    companion object {
    }

}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, companionObject, objectDeclaration,
primaryConstructor, propertyDeclaration, starProjection */
