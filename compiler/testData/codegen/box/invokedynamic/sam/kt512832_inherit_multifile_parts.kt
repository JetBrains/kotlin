// INHERIT_MULTIFILE_PARTS
// SAM_CONVERSIONS: INDY
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_STDLIB

// FILE: org/example/foo/Foo.java
package org.example.foo;
public interface Foo {
    String foo(String s);
}

// FILE: bar1.kt

@file:JvmMultifileClass
@file:JvmName("Bar")

package org.example.bar

fun doFoo(s: String): String = s.toUpperCase()

// FILE: bar2.kt

@file:JvmMultifileClass
@file:JvmName("Bar")

package org.example.bar

const val unused = 1

// FILE: Baz.kt

package org.example.baz

import org.example.foo.Foo
import org.example.bar.doFoo

fun box(): String {
    val foo = Foo(::doFoo)
    return foo.foo("ok")
}