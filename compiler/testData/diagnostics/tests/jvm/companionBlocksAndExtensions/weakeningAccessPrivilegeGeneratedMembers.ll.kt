// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `ACCIDENTAL_OVERRIDE` and `CANNOT_WEAKEN_ACCESS_PRIVILEGE_FOR_STATIC_MEMBER`.
// LL_FIR_DIVERGENCE
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
        protected fun jvmStatic() {}
        protected fun jvmStaticOverloads() {}
        protected fun jvmOverloads() {}
    }
}

open class ChildOfBaseWithGenerated : BaseWithGenerated()

class HidesGeneratedInGrandparent : ChildOfBaseWithGenerated() {
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
        @JvmStatic fun jvmStatic() {}
        @JvmStatic @JvmOverloads fun jvmStaticOverloads(x: Int = 0) {}
    }

    companion {
        @JvmOverloads fun jvmOverloads(x: Int = 0) {}
        fun introducedAt(@IntroducedAt("1.1") x: Int = 0) {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, objectDeclaration,
stringLiteral */
