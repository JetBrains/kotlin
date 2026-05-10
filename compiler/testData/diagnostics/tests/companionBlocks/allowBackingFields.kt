// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +CompanionBlocksAndExtensions
// WITH_STDLIB
class C {
    companion {
        val foo = 1
        const val bar = 1
        @JvmField
        val baz = 1
    }
}


/* GENERATED_FIR_TAGS: const, integerLiteral, propertyDeclaration, propertyWithExtensionReceiver */
