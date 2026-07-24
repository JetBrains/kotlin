// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: a.kt
package a

class C {
    companion {
        fun insideCompanion() {
            foo.add(1)
        }
    }

    companion object {
       fun insideCompanionObject() {
            foo.add(1)
        }
    }

    fun insideClass() {
        foo.add(1)
    }
}

companion val C.foo: List<Int>
    field: MutableList<Int> = TODO()

companion fun C.bar() {
    foo.add(1)
}

fun test() {
    C.foo.add(1)
}

// FILE: a2.kt
package a

fun test2(){
    C.foo.<!UNRESOLVED_REFERENCE!>add<!>(1)
}

// FILE: b.kt
package b

import a.*

fun test(){
    C.foo.<!UNRESOLVED_REFERENCE!>add<!>(1)
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, explicitBackingField, funWithExtensionReceiver,
functionDeclaration, integerLiteral, objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, smartcast */
