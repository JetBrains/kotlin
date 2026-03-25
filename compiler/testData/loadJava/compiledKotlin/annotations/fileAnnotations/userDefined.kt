// IGNORE_FIR_METADATA_LOADING_K1: ANY
// IGNORE_FIR_METADATA_LOADING_K2: JVM_IR
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

fun bar() = "bar"
