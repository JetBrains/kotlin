// !API_VERSION: 1.3
// !JVM_DEFAULT_MODE: compatibility
// JVM_TARGET: 1.8

interface KInterface  {
    @JvmDefault
    fun test2(): String {
        return "OK"
    }
}

interface KInterface2 : KInterface  {
    @JvmDefault
    abstract override fun test2(): String
}

// 1 INVOKESTATIC KInterface.access\$test2\$jd
// +
// 0 INVOKESTATIC KInterface2.access\$test2\$jd
// =
// 1 INVOKESTATIC

// 1 INVOKESPECIAL KInterface.test2
// 0 INVOKESPECIAL KInterface2.test2
