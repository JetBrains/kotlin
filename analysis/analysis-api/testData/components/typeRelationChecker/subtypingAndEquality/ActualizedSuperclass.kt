// IGNORE_FE10

// MODULE: common
// FILE: common.kt
package test

expect class Base<T>

class Foo : Base<String>

// MODULE: main()()(common)

// ARE_EQUAL: false
// ARE_EQUAL_LENIENT: false
// IS_SUBTYPE: true
// IS_SUBTYPE_LENIENT: true

// SUPERCLASS_ID: test/Base
// IS_CLASS_SUBTYPE: true
// IS_CLASS_SUBTYPE_LENIENT: true

// FILE: main.kt
package test

actual class Base<T>

val f<caret_type1>oo: Foo = Foo()

val b<caret_type2>ase: Base<String> = Foo()
