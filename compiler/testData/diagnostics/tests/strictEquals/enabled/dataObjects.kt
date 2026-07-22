// RUN_PIPELINE_TILL: FRONTEND

// MODULE: m1

data object Regular1 {
    val x: Int = 42
}

interface Interface {
    val x: CharSequence
    override fun equals(@EqualityBound(Interface::class) other: Any?): Boolean
}

data object Inherited1 : Interface {
    override val x: String = "!"
}

open class WithFinalEquals {
    open val x: CharSequence = "!"
    final override fun equals(@EqualityBound(WithFinalEquals::class) other: Any?): Boolean = <!USELESS_IS_CHECK!>other is WithFinalEquals<!>
}

data object NotGenerated1 : WithFinalEquals() {
    override val x: String = "!"
}

// MODULE: m2(m1)

data object Regular2 {
    val x: Int = 42
}

data object Inherited2 : Interface {
    override val x: String = "!"
}

data object NotGenerated2 : WithFinalEquals() {
    override val x: String = "!"
}

fun useSite(nany: Any?) {
    if (Regular1 == nany) {
        nany.x
    }
    if (Regular2 == nany) {
        nany.x
    }
    if (Inherited1 == nany) {
        nany.x.length
        nany.x.plus("!")
    }
    if (Inherited2 == nany) {
        nany.x.length
        nany.x.plus("!")
    }
    if (NotGenerated1 == nany) {
        nany.x.length
        nany.x.<!UNRESOLVED_REFERENCE!>plus<!>("!")
    }
    if (NotGenerated2 == nany) {
        nany.x.length
        nany.x.<!UNRESOLVED_REFERENCE!>plus<!>("!")
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, data, equalityExpression, functionDeclaration, integerLiteral,
interfaceDeclaration, isExpression, nullableType, objectDeclaration, operator, override, propertyDeclaration, smartcast,
stringLiteral */
