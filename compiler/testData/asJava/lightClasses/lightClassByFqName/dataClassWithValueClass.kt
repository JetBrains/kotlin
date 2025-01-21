// one.MyDataClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package one

@JvmInline
value class MyValueClass(val str: String)

data class MyDataClass(val value: MyValueClass)