interface I {
    enum class E {
        V { override fun go() { } };
        abstract fun go()
    }
}

// TESTED_OBJECT_KIND: innerClass
// TESTED_OBJECTS: I, E
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_ABSTRACT, ACC_ENUM