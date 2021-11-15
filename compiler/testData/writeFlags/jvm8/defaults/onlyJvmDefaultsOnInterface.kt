// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Test {
    @JvmDefault
    fun foo(): String {
        return "OK"
    }

    @JvmDefault
    fun bar(x: String = "OK"): String {
        return x
    }
}


// TESTED_OBJECT_KIND: innerClass
// TESTED_OBJECTS: Test, DefaultImpls
// ABSENT: TRUE