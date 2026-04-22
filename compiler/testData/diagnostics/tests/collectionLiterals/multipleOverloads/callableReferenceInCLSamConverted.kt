// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals, +CompanionBlocksAndExtensions
// WITH_STDLIB

class A {
    fun interface Sam {
        fun foo(): String
    }

    companion {
        operator fun of(vararg sams: Sam): A = A()
    }
}

class B {
    fun interface Sam {
        fun foo(): Int
    }

    companion object {
        operator fun of(vararg sams: Sam): B = B()
    }
}

class C {
    companion {
        operator fun of(vararg crs: () -> String): C = C()
    }
}

fun ab(a: A) { }
fun ab(b: B) { }

fun bc(b: B) { }
fun bc(c: C) { }

fun ac(a: A) { }
fun ac(c: C) { }

fun acList(a: List<A.Sam>) { }
fun acList(a: List<() -> String>) { }

fun makeString(): String = ""
fun <T: Number> materializeNumber(): T = null!!
fun <T> materialize(): T = null!!

fun ms(): Int = 42

fun test(t: Any) {
    fun ms(): Set<*> = ["!"]

    ab([::makeString])
    ab([::ms])
    ab([::materializeNumber])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>ab<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>]<!>)

    bc([::makeString])
    bc([::ms])
    bc([::materializeNumber])
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>bc<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>]<!>)

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>ac<!>([::makeString])
    <!NONE_APPLICABLE!>ac<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>materializeNumber<!>]<!>)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>ac<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>]<!>)

    acList([::makeString])
    <!NONE_APPLICABLE!>acList<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>materializeNumber<!>]<!>)
    acList([::materialize])
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, funInterface, functionDeclaration,
interfaceDeclaration, nestedClass, objectDeclaration, operator, samConversion, stringLiteral, vararg */
