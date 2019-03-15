// JVM_TARGET: 1.8

interface Test {
    fun test(): String {
        return "OK"
    }

    fun testAbstract(): String
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, test
// FLAGS: ACC_PUBLIC, ACC_ABSTRACT

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, testAbstract
// FLAGS: ACC_PUBLIC, ACC_ABSTRACT
