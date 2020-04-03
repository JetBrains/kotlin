// FILE: foo.kt

// FILE: foo.kt

package test

typealias ClassAlias = ClassSample
typealias ObjectAlias = ObjectSample
typealias EnumAlias = EnumSample

class ClassSample

object ObjectSample

enum class EnumSample {
    Entry;
}

// FILE: bar.kt

import test.ClassAlias.*
import test.ObjectAlias.*
import test.EnumAlias.*
import test.EnumAlias


fun bar() {
    Entry
    EnumAlias.Entry
}