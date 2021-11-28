// WITH_EXTENDED_CHECKERS
// MODULE: m1
// FILE: base.kt

package hello

class Hello(val msg: String)

class Test(val set: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.Set<*><!>)

// MODULE: m2(m1)
// FILE: user.kt

package test

import hello.Hello

fun foo(hello: Hello): String = hello.msg

