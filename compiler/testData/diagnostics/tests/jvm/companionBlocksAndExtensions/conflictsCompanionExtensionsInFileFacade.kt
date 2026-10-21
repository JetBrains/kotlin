// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER

// Companion extensions are statics of the file facade without a receiver parameter,
// so they can clash with other declarations of the same file.

class A
class B

<!CONFLICTING_JVM_DECLARATIONS!>companion fun A.vsTopLevel()<!> = 1
<!CONFLICTING_JVM_DECLARATIONS!>fun vsTopLevel()<!> = 2

// companion fun A.f(a: A) and fun A.f() are both f(LA;) in the facade.
<!CONFLICTING_JVM_DECLARATIONS!>companion fun A.vsRegularExtension(a: A)<!> = 1
<!CONFLICTING_JVM_DECLARATIONS!>fun A.vsRegularExtension()<!> = 2

// Different receivers, same JVM signature.
<!CONFLICTING_JVM_DECLARATIONS!>companion fun A.vsOtherReceiver()<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>companion fun B.vsOtherReceiver()<!> {}

// Resolved with @JvmName.
companion fun A.make() {}
@JvmName("makeB")
companion fun B.make() {}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, stringLiteral */
