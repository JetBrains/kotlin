// !JVM_DEFAULT_MODE: all-compatibility
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Test {
    val test: String
        get() = "OK"

    var test2: String
        get() = "OK"
        set(field) {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, access$getTest$jd
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, access$getTest2$jd
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, access$setTest2$jd
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC
