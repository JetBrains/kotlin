// RUN_PIPELINE_TILL: FRONTEND

// MODULE: m1

data class Regular1(val x: Int)

data class ExplicitAnnotated1(val x: Int) {
    override fun equals(@EqualityBound(ExplicitAnnotated1::class) other: Any?): Boolean {
        return x == other.x
    }
}

data class Explicit1(val x: Int) {
    override fun equals(other: Any?): Boolean {
        return other is Explicit1 && x == other.x
    }
}

interface Interface {
    val x: CharSequence
    override fun equals(@EqualityBound(Interface::class) other: Any?): Boolean
}

data class ExplicitInherited1(override val x: String) : Interface {
    override fun equals(other: Any?): Boolean {
        return x == other.x
    }
}

open class WithFinalEquals {
    open val x: CharSequence = "!"
    final override fun equals(@EqualityBound(WithFinalEquals::class) other: Any?): Boolean = <!USELESS_IS_CHECK!>other is WithFinalEquals<!>
}

data class NotGenerated1(override val x: String) : WithFinalEquals()

data class Generic1<T : CharSequence>(val x: T)

// MODULE: m2(m1)

data class Regular2(val x: Int)

data class ExplicitAnnotated2(val x: Int) {
    override fun equals(@EqualityBound(ExplicitAnnotated2::class) other: Any?): Boolean {
        return x == other.x
    }
}

data class Explicit2(val x: Int) {
    override fun equals(other: Any?): Boolean {
        return other is Explicit2 && x == other.x
    }
}

data class ExplicitInherited2(override val x: String) : Interface {
    override fun equals(other: Any?): Boolean {
        return x == other.x
    }
}

data class NotGenerated2(override val x: String) : WithFinalEquals()

data class Generic2<T : CharSequence>(val x: T)

fun useSite(
    r1: Regular1,
    r2: Regular2,
    ea1: ExplicitAnnotated1,
    ea2: ExplicitAnnotated2,
    e1: Explicit1,
    e2: Explicit2,
    ei1: ExplicitInherited1,
    ei2: ExplicitInherited2,
    ng1: NotGenerated1,
    ng2: NotGenerated2,
    gen1: Generic1<String>,
    gen2: Generic2<String>,
    nany: Any?,
) {
    if (r1 == nany) {
        nany.x
    }
    if (r2 == nany) {
        nany.x
    }
    if (ea1 == nany) {
        nany.x
    }
    if (ea2 == nany) {
        nany.x
    }
    if (e1 == nany) {
        nany.<!UNRESOLVED_REFERENCE!>x<!>
    }
    if (e2 == nany) {
        nany.<!UNRESOLVED_REFERENCE!>x<!>
    }
    if (ei1 == nany) {
        nany.x.length
        nany.x.<!UNRESOLVED_REFERENCE!>plus<!>("!")
    }
    if (ei2 == nany) {
        nany.x.length
        nany.x.<!UNRESOLVED_REFERENCE!>plus<!>("!")
    }
    if (ng1 == nany) {
        nany.x.length
        nany.x.<!UNRESOLVED_REFERENCE!>plus<!>("!")
    }
    if (ng2 == nany) {
        nany.x.length
        nany.x.<!UNRESOLVED_REFERENCE!>plus<!>("!")
    }
    if (gen1 == nany) {
        nany.x.length
        nany.x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>("!")
    }
    if (gen2 == nany) {
        nany.x.length
        nany.x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>plus<!>("!")
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, classReference, data, equalityExpression, functionDeclaration,
ifExpression, interfaceDeclaration, isExpression, nullableType, operator, override, primaryConstructor,
propertyDeclaration, smartcast, stringLiteral */
