// JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
// FULL_JDK

interface KInterface {

    fun test(s: String ="OK"): String {
        return s
    }
}

// 1 INVOKESTATIC KInterface.access\$test\$jd
// 1 INVOKESTATIC KInterface.test\$default

// from $default
// 1 INVOKEINTERFACE KInterface.test

//from $jd
// 1 INVOKESPECIAL KInterface.test
