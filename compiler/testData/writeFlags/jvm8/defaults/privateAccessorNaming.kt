// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// !JVM_DEFAULT_MODE: enable
// JVM_TARGET: 1.8
// WITH_STDLIB
interface I {

    @JvmDefault
    private fun foo() = 4

    fun bar() = { foo() + 5 }()

}


// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: I, access$foo
// FLAGS: ACC_PUBLIC, ACC_STATIC, ACC_SYNTHETIC