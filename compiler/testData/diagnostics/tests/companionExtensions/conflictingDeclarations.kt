// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
// DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS
// WITH_STDLIB

companion fun String.foo() {}

@JvmName("fooInt")
companion fun Int.foo() {}

fun String.bar() {}
companion fun String.bar() {}

companion val String.fooProp get() = 1

@get:JvmName("fooIntProp")
companion val Int.fooProp get() = 1

val String.barProp get() = 1
companion val String.barProp get() = 1

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, stringLiteral */
