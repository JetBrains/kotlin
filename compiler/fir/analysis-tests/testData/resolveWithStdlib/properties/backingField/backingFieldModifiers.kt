// RUN_PIPELINE_TILL: FRONTEND
class A {
    val a: Number
        <!WRONG_MODIFIER_TARGET!>abstract<!> field = 1

    val b: Number
        <!WRONG_MODIFIER_TARGET!>open<!> field = 1

    val c: Number
        <!WRONG_MODIFIER_TARGET!>final<!> field = 1

    val d: Number
        <!WRONG_MODIFIER_TARGET!>inline<!> field = 1

    val e: Number
        <!WRONG_MODIFIER_TARGET!>noinline<!> field = 1

    val f: Number
        <!WRONG_MODIFIER_TARGET!>crossinline<!> field = 1

    val g: Number
        <!WRONG_MODIFIER_TARGET!>tailrec<!> field = 1

    val h: Number
        <!WRONG_MODIFIER_TARGET!>const<!> field = 1

    val i: Number
        <!LATEINIT_FIELD_IN_VAL_PROPERTY, LATEINIT_PROPERTY_FIELD_DECLARATION_WITH_INITIALIZER, WRONG_MODIFIER_TARGET!>lateinit<!> field = 1
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, integerLiteral, propertyDeclaration */
