// RUN_PIPELINE_TILL: CODEGEN

sealed class C {
    object Obj : C()

    companion object {
        val ObjAsProp: C = Obj
    }
}

val csr1: C = Obj<Unresolved>
val csr2: C = ObjAsProp<Unit>

@Target(FIELD<InCraArgument>)
annotation class Anno

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, nestedClass, objectDeclaration,
propertyDeclaration, sealed */
