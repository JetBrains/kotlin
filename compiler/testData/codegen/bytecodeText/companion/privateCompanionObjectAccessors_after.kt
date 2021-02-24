// !LANGUAGE: +ProperVisibilityForCompanionObjectInstanceField

class Host {
    private companion object {
        fun foo() = 1
    }

    fun test() = { foo() }
}

// 1 synthetic access\$