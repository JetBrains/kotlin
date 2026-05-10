// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// LANGUAGE: +ProhibitCallableReferencesToStaticsWithTypeArgumentsOrNullMarkInLhs

open class A {
    inner class B
}

class G<T> {
    companion object : A() { }
}

typealias GTA<T> = G<T>
typealias NGTA = G<String>

fun test() {
    G::B
    G<String>::<!UNRESOLVED_REFERENCE!>B<!>
    GTA::B
    GTA<String>::<!UNRESOLVED_REFERENCE!>B<!>
    NGTA::B
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, companionObject, functionDeclaration, inner, nullableType,
objectDeclaration, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
