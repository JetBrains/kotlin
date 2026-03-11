// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions
// RENDER_DIAGNOSTIC_ARGUMENTS
class C {
    companion {
        class <!ILLEGAL_COMPANION_BLOCK_MEMBER("class")!>Nested<!>

        <!ILLEGAL_COMPANION_BLOCK_MEMBER("type alias")!>typealias TA = Int<!>

        <!ILLEGAL_COMPANION_BLOCK_MEMBER("constructor")!>constructor(x: Int)<!> : this()

        <!ILLEGAL_COMPANION_BLOCK_MEMBER("initializer")!>init<!> {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, init, nestedClass, secondaryConstructor, typeAliasDeclaration */
