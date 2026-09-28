// RUN_PIPELINE_TILL: CODEGEN
interface A {
  fun foo() {}
}

interface B : A {}

class C(b : B) : B by b {

}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration,
primaryConstructor */
