// RUN_PIPELINE_TILL: CODEGEN
open error class A
abstract error class B
sealed error class C

class SubA : A()
class SubB : B()
class SubC : C()

/* GENERATED_FIR_TAGS: classDeclaration, sealed */
