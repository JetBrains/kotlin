// !API_VERSION: 1.3
// !ENABLE_JVM_DEFAULT
// JVM_TARGET: 1.8
// WITH_RUNTIME

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
