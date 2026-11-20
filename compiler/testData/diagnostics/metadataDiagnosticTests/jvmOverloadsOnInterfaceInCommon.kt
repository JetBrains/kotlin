// METADATA_TARGET_PLATFORMS: JVM, Native
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// ISSUE: KTIJ-36296
import kotlin.jvm.JvmOverloads

interface CommonInterfaceContainer {
    <!OVERLOADS_INTERFACE!>@JvmOverloads<!>
    fun commonFaceFun(p1: Int = 0)
}
