// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

private fun createPrivateObject() =
        object {
            fun foo(): String = "foo"
        }

private val privatePropertyWithChainedObject =
        object {
            fun bar() = "bar"
        }.also { }

private val privatePropertyWithNestedObject =
        object {
            fun baz() = "baz"
        }

val publicRefToChained = privatePropertyWithChainedObject.bar()

val publicRefToNested = privatePropertyWithNestedObject.baz()

fun useAnonObject(): String {
    return createPrivateObject().foo() + publicRefToChained + publicRefToNested
}


/* GENERATED_FIR_TAGS: functionDeclaration, propertyDeclaration */
