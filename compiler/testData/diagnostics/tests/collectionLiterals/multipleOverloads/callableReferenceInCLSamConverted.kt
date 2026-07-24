// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals, +CompanionBlocks +CompanionExtensions
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

fun ab(a: A) = 1
fun ab(b: B) = "A"

fun bc(b: B) = 1
fun bc(c: C) = "A"

fun ac(a: A) = 1
fun ac(c: C) = "A"

fun acList(a: List<A.Sam>) = 1
fun acList(a: List<() -> String>) = "A"

fun makeString(): String = ""
fun <T: Number> materializeNumber(): T = null!!
fun <T> materialize(): T = null!!

fun ms(): Int = 42

fun test(t: Any) {
    fun ms(): Set<*> = ["!"]

    val a1: Int = ab([::makeString])
    val a2: String = ab([::ms])
    val a3: String = ab([::materializeNumber])
    val a4: Int = <!OVERLOAD_RESOLUTION_AMBIGUITY!>ab<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>]<!>)

    val a5: String = bc([::makeString])
    val a6: Int = bc([::ms])
    val a7: Int = bc([::materializeNumber])
    val a8: Int = <!OVERLOAD_RESOLUTION_AMBIGUITY!>bc<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>]<!>)

    val a9: Int = <!OVERLOAD_RESOLUTION_AMBIGUITY!>ac<!>([::makeString])
    val a10: Int = <!OVERLOAD_RESOLUTION_AMBIGUITY!>ac<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>materializeNumber<!>]<!>)
    val a11: Int = <!OVERLOAD_RESOLUTION_AMBIGUITY!>ac<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[::<!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>]<!>)

    val a12: String = acList([::makeString])
    val a13: String = acList([::<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>materializeNumber<!>])
    val a14: String = acList([::materialize])
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, funInterface, functionDeclaration,
interfaceDeclaration, nestedClass, objectDeclaration, operator, samConversion, stringLiteral, vararg */
