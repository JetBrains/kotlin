// pack.OtherClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package pack

class OtherClass(val svc: SimpleValueClass)

@JvmInline
value class SimpleValueClass(val value: Int)
