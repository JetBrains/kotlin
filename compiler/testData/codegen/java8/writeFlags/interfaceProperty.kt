// JVM_TARGET: 1.8

interface Test {
    var z: String
        get() = "OK"
        set(value) {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, getZ
// FLAGS: ACC_PUBLIC, ACC_ABSTRACT

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, setZ
// FLAGS: ACC_PUBLIC, ACC_ABSTRACT
