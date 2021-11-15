// !LANGUAGE: +UseGetterNameForPropertyAnnotationsMethodOnJvm
// WITH_STDLIB
class Foo {
    annotation class Anno

    @Anno
    @get:JvmName("jvmName")
    val prop: Int
        get() = 42
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo, jvmName$annotations
// FLAGS: ACC_DEPRECATED, ACC_STATIC, ACC_SYNTHETIC, ACC_PUBLIC
