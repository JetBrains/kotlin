// !JVM_DEFAULT_MODE: enable
// IGNORE_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface Test {
    @JvmDefault
    fun test(): String {
        return "OK"
    }

    fun testAbstract(): String

    fun testDefaultImpl() {

    }
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, test
// FLAGS: ACC_PUBLIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, testAbstract
// FLAGS: ACC_PUBLIC, ACC_ABSTRACT

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, testDefaultImpl
// FLAGS: ACC_PUBLIC, ACC_ABSTRACT

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test$DefaultImpls, test
// ABSENT: TRUE

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test$DefaultImpls, testAbstract
// ABSENT: TRUE

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test$DefaultImpls, testDefaultImpl
// FLAGS: ACC_PUBLIC, ACC_STATIC
