// RUN_PIPELINE_TILL: FRONTEND

sealed class C {
    object Obj : C()

    companion object {
        val ObjAsProp: C = Obj
    }
}

val csr1: C = Obj<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><<!UNRESOLVED_REFERENCE!>Unresolved<!>><!>
val csr2: C = ObjAsProp<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Unit><!>

@Target(FIELD<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><<!UNRESOLVED_REFERENCE!>InCraArgument<!>><!>)
annotation class Anno

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, nestedClass, objectDeclaration,
propertyDeclaration, sealed */
