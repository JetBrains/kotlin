// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_RUNTIME

annotation class Property(val value: String)

interface Test {
    @Property("OK")
    @JvmDefault
    val test: String
        get() = "OK"

    fun stub() {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, test$annotations
// ABSENT: TRUE

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test$DefaultImpls, test$annotations
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC, ACC_DEPRECATED
