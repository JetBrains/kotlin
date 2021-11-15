// !LANGUAGE: -GenerateJvmOverloadsAsFinal
// WITH_STDLIB

open class Foo {
    @JvmOverloads
    open fun bar(x: Int = 42, y: Int = -1): Int = x + y
}

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo, bar, (II)I
// FLAGS: ACC_PUBLIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo, bar, (I)I
// FLAGS: ACC_PUBLIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: Foo, bar, ()I
// FLAGS: ACC_PUBLIC
