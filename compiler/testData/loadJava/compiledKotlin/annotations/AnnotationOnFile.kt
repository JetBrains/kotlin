// PLATFORM_DEPENDANT_METADATA
// ^ Serializing file annotations to metadata is not implemented in Kotlin/JVM. See OSIP-1095.

@file:MyFileAnnotationSource("hello")
@file:MyFileAnnotationBinary("world")
@file:MyFileAnnotationRuntime("!")

package test

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class MyFileAnnotationSource(val value: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class MyFileAnnotationBinary(val value: String)

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class MyFileAnnotationRuntime(val value: String)

class A

typealias B = A

fun foo(x: Int) = 42

val bar: Int
    get() = 42
