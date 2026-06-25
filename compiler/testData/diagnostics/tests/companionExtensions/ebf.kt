// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: a.kt
package a

class C {
    companion {
        fun insideCompanion() {
            <!SMARTCAST_IMPOSSIBLE!>foo<!>.add(1)
        }
    }

    companion object {
       fun insideCompanionObject() {
            <!SMARTCAST_IMPOSSIBLE!>foo<!>.add(1)
        }
    }

    fun insideClass() {
        <!SMARTCAST_IMPOSSIBLE!>foo<!>.add(1)
    }
}

companion val C.foo: List<Int>
    <!EXPLICIT_BACKING_FIELD_IN_EXTENSION!>field<!>: MutableList<Int> = TODO()

companion fun C.bar() {
    <!SMARTCAST_IMPOSSIBLE!>foo<!>.add(1)
}

fun test() {
    <!SMARTCAST_IMPOSSIBLE!>C.foo<!>.add(1)
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
