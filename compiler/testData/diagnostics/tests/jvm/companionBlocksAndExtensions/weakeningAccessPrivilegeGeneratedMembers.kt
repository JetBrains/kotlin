// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +CompanionBlocks +JvmProhibitWeakeningAccessPrivilegeForStatics
// ISSUE: KT-90567

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
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> fun jvmStatic() {}
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> fun jvmStaticOverloads() {}
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> fun jvmOverloads() {}
    }
}

open class ChildOfBaseWithGenerated : BaseWithGenerated()

class HidesGeneratedInGrandparent : ChildOfBaseWithGenerated() {
    companion {
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> fun jvmStatic() {}
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> fun jvmStaticOverloads() {}
        <!CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER!>protected<!> fun jvmOverloads() {}
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
