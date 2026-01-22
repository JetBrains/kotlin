// JVM_DEFAULT_MODE: disable
// LANGUAGE: +JvmEnhancedBridges

interface I1 {
    fun foo(): Unit
}

interface I2: I1 {
    override fun foo(): Nothing
    // void foo() bridge to the java.lang.Void foo()
}

interface I3: I1 {
    override fun foo(): Unit
    // no bridges
}

interface I4: I1, I2, I3 {
    override fun foo(): Nothing
    // void foo() bridge to the java.lang.Void foo()
}


