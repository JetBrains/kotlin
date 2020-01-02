// !LANGUAGE: +MultiPlatformProjects +LateinitTopLevelProperties
// MODULE: m1-common
// FILE: common.kt

expect val justVal: String
expect var justVar: String

expect val String.extensionVal: Unit
expect var <T> T.genericExtensionVar: T

expect val valWithGet: String
    get
expect var varWithGetSet: String
    get set

expect var varWithPlatformGetSet: String
    expect get
    expect set

expect val backingFieldVal: String = "no"
expect var backingFieldVar: String = "no"

expect val customAccessorVal: String
    get() = "no"
expect var customAccessorVar: String
    get() = "no"
    set(value) {}

expect const val constVal: Int

expect lateinit var lateinitVar: String

expect val delegated: String by Delegate
object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }

fun test(): String {
    expect val localVariable: String
    localVariable = "no"
    return localVariable
}
