// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER` and `ACCIDENTAL_OVERRIDE`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +JvmProhibitWeakeningAccessPrivilegeForStatics
// ISSUE: KT-90567

// MODULE: lib
// FILE: lib.kt
@JvmInline
value class I(val value: Int)

open class A {
    companion {
        fun publicFoo() {}
        suspend fun publicSuspendWithInlineFoo(i: I) {}
        protected fun protectedFoo() {}
        internal fun internalFoo() {}
        private fun privateFoo() {}
        @PublishedApi internal fun publishedApiFoo() {}

        @JvmName("publicBar") fun A__UniqueName() {}
        fun publicBaz() {}

        val publicVal = ""
        val publicValWithInline = I(0)
        val publicDelegatedVal by lazy { "" }
        protected val protectedVal = ""
        internal val internalVal = ""
        private val privateVal = ""
        @PublishedApi internal val publishedApiVal = ""
        var publicVar = ""
        var publicValWithInlinr = I(0)
        protected var protectedVar = ""
        internal var internalVar = ""
        private var privateVar = ""
        @PublishedApi internal var publishedApiVar = ""
    }
}

// MODULE: test(lib)
// FILE: main.kt

class B1 : A() {
    companion {
        fun publicFoo() {}
        suspend fun publicSuspendWithInlineFoo(i: I) {}
        fun protectedFoo() {}
        fun internalFoo() {}
        fun privateFoo() {}
        fun publishedApiFoo() {}

        fun publicBar() {}
        @JvmName("publicBaz") fun B1__UniqueName() {}

        val publicVal = ""
        val publicValWithInline = I(0)
        val publicDelegatedVal by lazy { "" }
        val protectedVal = ""
        val internalVal = ""
        val privateVal = ""
        val publishedApiVal = ""
        var publicVar = ""
        var publicValWithInlinr = I(0)
        var protectedVar = ""
        var internalVar = ""
        var privateVar = ""
        var publishedApiVar = ""
    }
}

class B2 : A() {
    companion {
        private fun publicFoo() {}
        private suspend fun publicSuspendWithInlineFoo(i: I) {}
        private fun protectedFoo() {}
        private fun internalFoo() {}
        private fun privateFoo() {}
        private fun publishedApiFoo() {}

        private fun publicBar() {}
        @JvmName("publicBaz") private fun B1__UniqueName() {}

        // Private properties don't get accessors, so no conflicts, except when delegated
        private val publicVal = ""
        private val publicValWithInline = I(0)
        private val publicDelegatedVal by lazy { "" }
        private val protectedVal = ""
        private val internalVal = ""
        private val privateVal = ""
        private val publishedApiVal = ""
        private var publicVar = ""
        private var publicValWithInlinr = I(0)
        private var protectedVar = ""
        private var internalVar = ""
        private var privateVar = ""
        private var publishedApiVar = ""
    }
}

class B3 : A() {
    companion {
        protected val publicVal = ""
        protected val publicValWithInline = I(0)
        protected val publicDelegatedVal by lazy { "" }
        protected val protectedVal = ""
        protected val internalVal = ""
        protected val privateVal = ""
        protected val publishedApiVal = ""
        protected var publicVar = ""
        protected var publicValWithInlinr = I(0)
        protected var protectedVar = ""
        protected var internalVar = ""
        protected var privateVar = ""
        protected var publishedApiVar = ""
    }
}

class B4 : A() {
    companion {
        var publicVar: String
            get() = ""
            private set(value) {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, lambdaLiteral, nullableType,
primaryConstructor, propertyDeclaration, propertyDelegate, setter, stringLiteral, suspend, value */
