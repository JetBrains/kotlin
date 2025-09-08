// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75553
// RENDER_DIAGNOSTICS_FULL_TEXT
// MODULE: a
// FILE: a.kt
interface MissedInterface {
    fun foo() = 1
}

// MODULE: b(a)
// FILE: b.kt
interface Intermediate : MissedInterface

class Implementation : Intermediate {
    val Implementation.onlyClassReferences: Implementation? get() = null

    val Intermediate.allHierarchy: MissedInterface? get() = null
}

// MODULE: c(b)
// FILE: c.kt
fun Implementation.test() {
    val x1 = <!MISSING_DEPENDENCY_SUPERCLASS!>onlyClassReferences<!>
    x1

    val x2 = <!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_SUPERCLASS, MISSING_DEPENDENCY_SUPERCLASS_WARNING!>allHierarchy<!>
    x1
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, getter, integerLiteral,
interfaceDeclaration, localProperty, nullableType, propertyDeclaration, propertyWithExtensionReceiver */
