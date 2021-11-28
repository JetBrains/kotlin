// !LANGUAGE: +RepeatableAnnotations
// !API_VERSION: LATEST
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8
// STDLIB_JDK8

package test

@Repeatable
@JvmRepeatable(As::class)
annotation class A(val value: String)

annotation class As(val value: Array<A>)

@A("a1")
@A("a2")
class Z
