// JVM_TARGET: 1.8
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM8_TARGET_WITH_DEFAULTS

interface Test {
    fun test(): String {
        return "OK"
    }

    fun testAbstract(): String
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, test
// FLAGS: ACC_PUBLIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, testAbstract
// FLAGS: ACC_PUBLIC, ACC_ABSTRACT
