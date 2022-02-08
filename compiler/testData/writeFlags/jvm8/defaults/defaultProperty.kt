// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_STDLIB

interface Test {
    @JvmDefault
    var z: String
        get() = "OK"
        set(value) {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, getZ
// FLAGS: ACC_PUBLIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, setZ
// FLAGS: ACC_PUBLIC
