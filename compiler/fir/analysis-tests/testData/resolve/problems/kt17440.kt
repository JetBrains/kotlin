// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-17440
// WITH_STDLIB

// KT-17440: provideDelegate should support generics inference

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class FooFactory {
    operator fun <V> provideDelegate(thisRef: Any, prop: KProperty<*>): ReadWriteProperty<Any, V> = TODO()
}

class Bar {
    companion object {
        val factory: FooFactory = TODO()
    }
    val delegated: Int by factory
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
propertyDeclaration, propertyDelegate, starProjection, typeParameter */
