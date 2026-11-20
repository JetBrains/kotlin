// RUN_PIPELINE_TILL: FRONTEND
// MODULE: top
open class Dependency
// MODULE: middle(top)
open class DependencyInheritor : Dependency()
// MODULE: bottom(middle)
fun <<!MISSING_DEPENDENCY_SUPERCLASS!>T : DependencyInheritor | RichError<!>> foo() {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, typeConstraint, typeParameter */
