// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField
// LAMBDAS: CLASS

class Host {
    private companion object {
        fun foo() = 1
    }

    fun test() = { foo() }
}

// 1 synthetic access\$
