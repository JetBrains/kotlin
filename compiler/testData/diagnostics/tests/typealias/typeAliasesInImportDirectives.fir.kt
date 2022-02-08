// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

// FILE: foo.kt

package test

typealias ClassAlias = ClassSample
typealias ObjectAlias = ObjectSample
typealias EnumAlias = EnumSample

class ClassSample {
    class Nested1
}

object ObjectSample {
    class Nested2
}

enum class EnumSample {
    Entry;

    class Nested3
}

// FILE: test.kt

import test.ClassAlias
import test.ClassAlias.Nested1

import test.ClassSample.Nested1.*
import test.ClassAlias.<!UNRESOLVED_IMPORT!>Nested1<!>.*

import test.ObjectAlias
import test.ObjectAlias.Nested2

import test.ObjectSample.Nested2.*
import test.ObjectAlias.<!UNRESOLVED_IMPORT!>Nested2<!>.*

import test.EnumAlias
import test.EnumAlias.Nested3

import test.EnumSample.Nested3.*
import test.EnumAlias.<!UNRESOLVED_IMPORT!>Nested3<!>.*

import test.EnumAlias.Entry

fun f() {}
