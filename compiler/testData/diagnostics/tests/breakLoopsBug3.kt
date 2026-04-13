// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71966

package a

abstract class A : <!CYCLIC_INHERITANCE_HIERARCHY!>C<!>() {
    abstract class Nested
}

abstract class C : <!CYCLIC_INHERITANCE_HIERARCHY!>A.Nested<!>()

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass */
