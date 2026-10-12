// LIBRARY_PLATFORMS: JVM
// FILE: Lib.kt
package lib

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Retained

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class SourceOnly

@get:Retained
@set:Retained
@get:SourceOnly
@set:SourceOnly
@get:Throws(java.io.IOException::class, java.lang.IllegalStateException::class)
@set:Throws(java.io.FileNotFoundException::class)
var value: String
    get() = "OK"
    set(value) {}
