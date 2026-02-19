// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

import kotlin.reflect.KProperty

class A {
    val a by MyProperty()

    fun test() {
        checkSubtype<Int>(a)
    }
}

class MyProperty<R> {
    operator fun getValue(thisRef: R, desc: KProperty<*>): Int = throw Exception("$thisRef $desc")
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
nullableType, operator, propertyDeclaration, propertyDelegate, starProjection, stringLiteral, typeParameter,
typeWithExtension */
