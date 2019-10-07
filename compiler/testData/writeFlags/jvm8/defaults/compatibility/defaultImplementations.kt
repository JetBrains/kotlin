// !JVM_DEFAULT_MODE: compatibility
// JVM_TARGET: 1.8
// WITH_RUNTIME

interface A {
    @JvmDefault
    fun foo(x: Int = 0): Int {
        return x
    }
}

// TESTED_OBJECT_KIND: innerClass
// TESTED_OBJECTS: A, DefaultImpls
// FLAGS: ACC_FINAL, ACC_STATIC, ACC_PUBLIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: A, foo$default
// FLAGS: ACC_PUBLIC, ACC_SYNTHETIC,  ACC_STATIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: A$DefaultImpls, foo$default
// FLAGS: ACC_PUBLIC, ACC_SYNTHETIC, ACC_STATIC