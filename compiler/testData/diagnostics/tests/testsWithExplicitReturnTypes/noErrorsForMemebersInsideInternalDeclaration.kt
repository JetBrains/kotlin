// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-51758

@PublishedApi
internal class SomeClass {
    private val somethingPrivate = "123"

    public val somethingPublic = "456"

    fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>foo<!>() = "789"
}

@PublishedApi
internal class Outer {
    class Inner {
        private val somethingPrivate = "123"

        public val somethingPublic = "456"

        fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>foo<!>() = "789"
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, propertyDeclaration, stringLiteral */
