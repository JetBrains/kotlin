// RUN_PIPELINE_TILL: FRONTEND
interface I<F, G>

val aImpl: A.Interface
    get() = null!!

object A : <!UNRESOLVED_REFERENCE!>Nested<!>(), <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>Interface<!> by aImpl, I<<!UNRESOLVED_REFERENCE!>Nested<!>, <!UNRESOLVED_REFERENCE!>Interface<!>> {

    class Nested

    interface Interface
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, getter, inheritanceDelegation, interfaceDeclaration,
nestedClass, nullableType, objectDeclaration, propertyDeclaration, typeParameter */
