// NB this multifile class should be called "TestPackage" due to the way codegen box tests work.
@file:JvmName("TestPackage")
@file:JvmMultifileClass
package test

fun foo(): String = bar()
fun bar(): String = qux()
fun qux(): String = "OK"

fun box(): String = foo()