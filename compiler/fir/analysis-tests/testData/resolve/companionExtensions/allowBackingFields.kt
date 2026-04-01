// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
companion val String.foo = 1
companion const val String.bar = 1
@JvmField
companion val String.baz = 1

/* GENERATED_FIR_TAGS: const, integerLiteral, propertyDeclaration, propertyWithExtensionReceiver */
