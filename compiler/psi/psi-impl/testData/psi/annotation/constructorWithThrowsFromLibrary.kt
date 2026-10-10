// LIBRARY_PLATFORMS: JVM
// FILE: Lib.kt
package lib

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class Retained

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class SourceOnly

class WithThrows
@Retained
@SourceOnly
@Throws(java.io.IOException::class, java.lang.IllegalStateException::class)
constructor(val value: String)
