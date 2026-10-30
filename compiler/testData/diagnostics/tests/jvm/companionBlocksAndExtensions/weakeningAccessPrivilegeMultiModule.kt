// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks
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

        <!ACCIDENTAL_OVERRIDE!>fun publicBar()<!> {}
        <!ACCIDENTAL_OVERRIDE!>@JvmName("publicBaz") fun B1__UniqueName()<!> {}

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
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>private<!> fun publicFoo() {}
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>private<!> suspend fun publicSuspendWithInlineFoo(i: I) {}
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>private<!> fun protectedFoo() {}
        private fun internalFoo() {}
        private fun privateFoo() {}
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>private<!> fun publishedApiFoo() {}

        <!ACCIDENTAL_OVERRIDE!><!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>private<!> fun publicBar()<!> {}
        <!ACCIDENTAL_OVERRIDE!>@JvmName("publicBaz") <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>private<!> fun B1__UniqueName()<!> {}

        // Private properties don't get accessors, so no conflicts, except when delegated
        private val publicVal = ""
        private val publicValWithInline = I(0)
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>private<!> val publicDelegatedVal by lazy { "" }
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
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> val publicVal = ""
        protected <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>val publicValWithInline = I(0)<!>
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> val publicDelegatedVal by lazy { "" }
        protected val protectedVal = ""
        protected val internalVal = ""
        protected val privateVal = ""
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> val publishedApiVal = ""
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> var publicVar = ""
        protected <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>var publicValWithInlinr = I(0)<!>
        protected var protectedVar = ""
        protected var internalVar = ""
        protected var privateVar = ""
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> var publishedApiVar = ""
    }
}

class B4 : A() {
    companion {
        var publicVar: String
            get() = ""
            <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>private<!> set(value) {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, getter, integerLiteral, lambdaLiteral, nullableType,
primaryConstructor, propertyDeclaration, propertyDelegate, setter, stringLiteral, suspend, value */
