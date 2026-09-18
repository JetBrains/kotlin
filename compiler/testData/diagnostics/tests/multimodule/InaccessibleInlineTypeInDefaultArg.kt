// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87904
// WITH_STDLIB

// MODULE: base
// FILE: base.kt

@JvmInline
value class AnInlineClass(val value: String)

// MODULE: intermediate(base)
// FILE: intermediate.kt

class DependencyClass(val parameter: AnInlineClass = AnInlineClass("default"))

fun dependencyFunction(parameter: AnInlineClass = AnInlineClass("default")) {
}

// MODULE: use(intermediate)
// FILE: use.kt

fun main() {
    <!MISSING_DEPENDENCY_CLASS_IN_PARAMETER_WITH_DEFAULT_VALUE!>DependencyClass<!>()
    <!MISSING_DEPENDENCY_CLASS_IN_PARAMETER_WITH_DEFAULT_VALUE!>dependencyFunction<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, value */
