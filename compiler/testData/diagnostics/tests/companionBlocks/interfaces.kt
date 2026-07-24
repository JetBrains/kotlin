// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
// ISSUE: KT-87360

interface I {
    companion {
        val x by lazy { 1 }

        val y: Int = 0
            get() = field

        <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun foo()<!>

        <!MUST_BE_INITIALIZED!>val bar: String<!>

        private val private1 = 1
        private var private2 = 2
        var private3 = 2
            private set

        var public = 1

        const val X = 1
        <!PRIVATE_CONST_IN_INTERFACE!>private<!> const val X_1 = 1
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> const val X_2 = 1
        internal const val X_3 = 1
    }
}

interface I1 {
    companion {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        private val X = 1
    }
}

interface I2 {
    companion {
        @JvmField
        val X = 1

        @JvmField
        val Y = 1
    }
}

interface I3 {
    companion {
        @JvmField
        val X = 1
    }
    companion object {
        @JvmField
        val Y = 1
    }
}

interface I4 {
    companion {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        val X = 1

        val Y = 1
    }
}

interface I5 {
    companion {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        val X = 1
    }
    companion object {
        val Y = 1
    }
}

interface I6 {
    companion {
        val X = 1
    }
    companion object {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        val Y = 1
    }
}

interface I7 {
    companion {
        const val X = 1
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        val Y = 1
    }
}

interface I8 {
    companion {
        const val X = 1
    }
    companion object {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        val Y = 1
    }
}

interface I9 {
    companion {
        <!INAPPLICABLE_JVM_FIELD!>@JvmField<!>
        val X = 1
    }
    companion object {
        const val Y = 1
    }
}
/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, integerLiteral, interfaceDeclaration, lambdaLiteral,
nullableType, propertyDeclaration, propertyDelegate */
