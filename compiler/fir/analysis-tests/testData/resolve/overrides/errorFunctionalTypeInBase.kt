// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

interface A {
    val x: (String) -> <!UNRESOLVED_REFERENCE!>Ay<!>
}
interface B : A {
    override val x: <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>(Int) -> Int<!>
}

/* GENERATED_FIR_TAGS: functionalType, interfaceDeclaration, override, propertyDeclaration */
