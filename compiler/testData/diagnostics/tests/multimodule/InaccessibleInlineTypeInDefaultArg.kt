// ISSUE: KT-87904
// WITH_STDLIB
// See also compiler/testData/codegen/boxJvm/inlineClasses/inaccessibleTypeInDefaultArg.kt

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
    DependencyClass()
    dependencyFunction()
}

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration, value */
