// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80606
// FIR_IDENTICAL
// DUMP_CFG

class Some {
    val x: Int

    val y: Any
        field = <!UNINITIALIZED_VARIABLE!>x<!> + 1

    val z: Any = <!UNINITIALIZED_VARIABLE!>x<!> + 1

    init {
        x = 1
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, classDeclaration, explicitBackingField, init, integerLiteral,
propertyDeclaration */
