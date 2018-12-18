// !JVM_DEFAULT_MODE: compatibility
// JVM_TARGET: 1.8

interface KInterface  {
    @JvmDefault
    fun test2(): String {
        return "OK"
    }
}

interface KInterface2 : KInterface  {

}

// 1 INVOKESTATIC KInterface2.access\$test2\$jd
// 1 INVOKESTATIC KInterface.access\$test2\$jd

// 1 INVOKESPECIAL KInterface2.test2
// 1 INVOKESPECIAL KInterface.test2
