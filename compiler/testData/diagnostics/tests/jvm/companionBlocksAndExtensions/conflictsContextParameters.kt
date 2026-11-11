// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_PARAMETER

// Context parameters become leading JVM parameters, so they clash with explicit parameters of the same type.

class Ctx
class K1
class K2

class InBlock {
    companion {
        <!CONFLICTING_JVM_DECLARATIONS!>context(c: Ctx) fun f()<!> {}
        <!CONFLICTING_JVM_DECLARATIONS!>fun f(c: Ctx)<!> {}
    }
}

class WithInstanceMember {
    <!CONFLICTING_JVM_DECLARATIONS!>fun g(c: Ctx)<!> {}

    companion {
        <!CONFLICTING_JVM_DECLARATIONS!>context(c: Ctx) fun g()<!> {}
    }
}

// Companion extensions in the file facade: different receivers, same context.
<!CONFLICTING_JVM_DECLARATIONS!>context(c: Ctx) companion fun K1.e()<!> {}
<!CONFLICTING_JVM_DECLARATIONS!>context(c: Ctx) companion fun K2.e()<!> {}

// No clash: different context types.
class NoClash {
    companion {
        context(c: Ctx) fun n() {}
        context(k: K1) fun n() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext */
