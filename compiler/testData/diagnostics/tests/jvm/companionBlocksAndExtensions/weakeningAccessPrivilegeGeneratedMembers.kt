// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks
// ISSUE: KT-90567

/*
 * Weakening access privelege by @JvmStatic/etc. members is not checked (they are reported as accidential overrides).
 * Weakening access privelege of @JvmStatic/etc. is allowed to be consistent with cross-module case.
 */

open class BaseWithGenerated {
    companion object {
        @JvmStatic fun jvmStatic() {}
        @JvmStatic @JvmOverloads fun jvmStaticOverloads(x: Int = 0) {}
    }

    companion {
        @JvmOverloads fun jvmOverloads(x: Int = 0) {}
    }
}

class HidesGenerated : BaseWithGenerated() {
    companion {
        protected fun jvmStatic() {}
        protected fun jvmStaticOverloads() {}
        protected fun jvmOverloads() {}
    }
}

open class Base {
    companion {
        protected fun jvmStatic() {}
        protected fun jvmStaticOverloads() {}
        protected fun jvmOverloads() {}
        protected fun introducedAt() {}
    }
}

class HidesByGenerated : Base() {
    companion object {
        @JvmStatic <!ACCIDENTAL_OVERRIDE!>fun jvmStatic() {}<!>
        @JvmStatic @JvmOverloads <!ACCIDENTAL_OVERRIDE!>fun jvmStaticOverloads(x: Int = 0) {}<!>
    }

    companion {
        @JvmOverloads <!ACCIDENTAL_OVERRIDE!>fun jvmOverloads(x: Int = 0) {}<!>
        <!ACCIDENTAL_OVERRIDE!>fun introducedAt(@IntroducedAt("1.1") x: Int = 0) {}<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, objectDeclaration,
stringLiteral */
