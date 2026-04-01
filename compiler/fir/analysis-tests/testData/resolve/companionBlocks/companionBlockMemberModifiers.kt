// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocksAndExtensions

abstract class C {
    companion {
        private fun private() {}
        protected fun protected() {}
        public fun public() {}
        internal fun internal() {}

        <!WRONG_MODIFIER_TARGET!>abstract<!> fun abstract()
        <!WRONG_MODIFIER_TARGET!>open<!> fun open() {}
        <!WRONG_MODIFIER_TARGET!>final<!> fun final() {}
        <!WRONG_MODIFIER_TARGET!>override<!> fun override() {}

        suspend fun suspend() {}
        <!EXTERNAL_DECLARATION_CANNOT_HAVE_BODY!>external<!> fun external() {}
        <!WRONG_MODIFIER_TARGET!>lateinit<!> fun lateinit() {}
        <!NO_TAIL_CALLS_FOUND!>tailrec<!> fun tailrec() { <!NON_TAIL_RECURSIVE_CALL!>tailrec<!>() }
        <!WRONG_MODIFIER_TARGET!>const<!> fun const() {}
        <!NOT_A_MULTIPLATFORM_COMPILATION, WRONG_MODIFIER_TARGET!>expect<!> <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun expect(): String<!>
        <!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> fun actual() {}
        <!NOTHING_TO_INLINE!>inline<!> fun inline() {}

        <!WRONG_MODIFIER_TARGET!>companion<!> fun companion() {}
    }
}

abstract class D {
    companion {
        private val private = 1
        protected val protected = 1
        public val public = 1
        internal val internal = 1

        <!WRONG_MODIFIER_TARGET!>abstract<!> val abstract: String
        <!WRONG_MODIFIER_TARGET!>open<!> val bar = 1
        <!WRONG_MODIFIER_TARGET!>final<!> val baz = 1
        <!WRONG_MODIFIER_TARGET!>override<!> val qux = 1

        <!WRONG_MODIFIER_TARGET!>suspend<!> val suspend = 1
        <!WRONG_MODIFIER_TARGET!>external<!> val external = 1
        lateinit var lateinit: Any
        <!WRONG_MODIFIER_TARGET!>tailrec<!> val tailrec = 1
        const val const = 1
        <!NOT_A_MULTIPLATFORM_COMPILATION, WRONG_MODIFIER_TARGET!>expect<!> val expect: String
        <!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> val actual = 1
        inline val inline get() = 1

        <!WRONG_MODIFIER_TARGET!>companion<!> val companion get() = 1
    }
}

/* GENERATED_FIR_TAGS: actual, classDeclaration, functionDeclaration, override, suspend */
