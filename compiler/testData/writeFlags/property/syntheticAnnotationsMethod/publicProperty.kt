// LANGUAGE: +UseGetterNameForPropertyAnnotationsMethodOnJvm

class Foo {
    annotation class Anno

    @Anno
    val prop = 42
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo, getProp$annotations
// FLAGS: ACC_DEPRECATED, ACC_STATIC, ACC_SYNTHETIC, ACC_PUBLIC
