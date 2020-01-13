// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// IGNORE_BACKEND: JVM_IR
// ^ TODO implement ProperVisibilityForCompanionObjectInstanceField feature support in JMV_IR

class Host {
    private companion object {
        fun foo() = 1
    }

    fun test() = { foo() }
}

// 1 synthetic access\$