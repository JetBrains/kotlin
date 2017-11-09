// JVM_TARGET: 1.8
// KOTLIN_CONFIGURATION_FLAGS: +JVM.JVM8_TARGET_WITH_DEFAULTS

interface Test {
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
