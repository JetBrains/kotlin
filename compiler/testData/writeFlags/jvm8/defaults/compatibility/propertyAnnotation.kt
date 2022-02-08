// !JVM_DEFAULT_MODE: compatibility
// !LANGUAGE: +UseGetterNameForPropertyAnnotationsMethodOnJvm
// JVM_TARGET: 1.8
// WITH_STDLIB

annotation class Property(val value: String)

interface Test {
    @Property("OK")
    @JvmDefault
    val test: String
        get() = "OK"
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test, getTest$annotations
// ABSENT: TRUE

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Test$DefaultImpls, getTest$annotations
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC, ACC_DEPRECATED
