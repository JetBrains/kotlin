// RUN_PIPELINE_TILL: FRONTEND

interface T {
  val a = <!PROPERTY_INITIALIZER_IN_INTERFACE!><!UNRESOLVED_REFERENCE!>Foo<!>.bar()<!>
}

/* GENERATED_FIR_TAGS: interfaceDeclaration, propertyDeclaration */
