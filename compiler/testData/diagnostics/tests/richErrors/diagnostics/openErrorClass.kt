// RUN_PIPELINE_TILL: FRONTEND
<!NON_FINAL_RICH_ERROR_CLASS!>open<!> error class A
<!NON_FINAL_RICH_ERROR_CLASS!>abstract<!> error class B
<!NON_FINAL_RICH_ERROR_CLASS!>sealed<!> error class C

class SubA : A()
class SubB : B()
class SubC : C()

/* GENERATED_FIR_TAGS: classDeclaration, sealed */
