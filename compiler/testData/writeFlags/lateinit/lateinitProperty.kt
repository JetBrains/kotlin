// IGNORE_BACKEND: JVM_IR
class My {
    lateinit var s: String
        private set
}

// TESTED_OBJECT_KIND: property
// TESTED_OBJECTS: My, s
// FLAGS: ACC_PRIVATE
