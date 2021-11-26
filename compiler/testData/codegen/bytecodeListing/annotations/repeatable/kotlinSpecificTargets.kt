// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8

@file:A("file1")
@file:A("file2")
package test

@Repeatable
@Target(AnnotationTarget.FILE, AnnotationTarget.TYPEALIAS)
annotation class A(val value: String)

@A("typealias1")
@A("typealias2")
typealias TA<X> = List<X>
