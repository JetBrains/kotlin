// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-37304

import kotlin.properties.ReadWriteProperty

interface B

class A {
    private fun <T> property(initialValue: T): ReadWriteProperty<A, T> = null!!

    var conventer by property<(B) -> B>({ it })
    var conventerWithExpectedType: (B) -> B by property({ it })
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, nullableType, propertyDeclaration, propertyDelegate, setter, typeParameter */
