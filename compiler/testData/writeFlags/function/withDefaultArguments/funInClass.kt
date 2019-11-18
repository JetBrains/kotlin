open class MyClass() {
    fun testPublic(s: String = "") {}

    protected fun testProtected(s: String = "") {}

    private fun testPrivate(s: String = "") {}
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, testPublic$default
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, testProtected$default
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: MyClass, testPrivate$default
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC