// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-79672
// LANGUAGE: +DataFlowBasedExhaustiveness
// WITH_STDLIB

// FILE: ClassesIsChecks.kt
package classesIsChecks

fun checkSomething(stuff: Stuff) {
    require(stuff is Stuff.ObjA || stuff is Stuff.ObjB) {
        "ObjC not supported here"
    }

    when (stuff) {
        is Stuff.ObjA -> println("ObjA")
        is Stuff.ObjB -> println("ObjB")
    }
}

sealed interface Stuff {
    data object ObjA : Stuff
    data object ObjB : Stuff
    data object ObjC : Stuff
}

// FILE: ClassesEqChecks.kt
package classesEqChecks

fun checkSomething(stuff: Stuff) {
    require(stuff == Stuff.ObjA || stuff == Stuff.ObjB) {
        "ObjC not supported here"
    }

    when (stuff) {
        Stuff.ObjA -> println("ObjA")
        Stuff.ObjB -> println("ObjB")
    }
}

sealed interface Stuff {
    data object ObjA : Stuff
    data object ObjB : Stuff
    data object ObjC : Stuff
}

// FILE: Enums.kt
package enums

fun checkSomething(stuff: Stuff) {
    require(stuff == Stuff.ObjA || stuff == Stuff.ObjB) {
        "ObjC not supported here"
    }

    when (stuff) {
        Stuff.ObjA -> println("ObjA")
        Stuff.ObjB -> println("ObjB")
    }
}

enum class Stuff { ObjA, ObjB, ObjC }

/* GENERATED_FIR_TAGS: data, disjunctionExpression, enumDeclaration, enumEntry, equalityExpression, functionDeclaration,
interfaceDeclaration, isExpression, lambdaLiteral, nestedClass, objectDeclaration, sealed, smartcast, stringLiteral,
whenExpression, whenWithSubject */
