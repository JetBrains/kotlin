// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

//KT-1944 Inference fails on run()
package j

import checkSubtype

class P {
    var x : Int = 0
        private set

    fun foo() {
        val r = run {x = 5} // ERROR
        checkSubtype<Unit>(r)
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, integerLiteral, lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
