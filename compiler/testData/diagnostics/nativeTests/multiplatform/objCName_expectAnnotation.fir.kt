// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect annotation class MyObjcName(val name: String = "", val swiftName: String = "", val exact: Boolean = false)

@MyObjcName("ObjCClass", "SwiftClass")
open class KotlinClass {
    @MyObjcName("objCProperty")
    open var kotlinProperty: Int = 0
    @MyObjcName(swiftName = "swiftFunction")
    open fun @receiver:MyObjcName("objCReceiver") Int.kotlinFunction(
        @MyObjcName("objCParam") kotlinParam: Int
    ): Int = this + kotlinParam
}

@MyObjcName("ObjCSubClass", "SwiftSubClass")
class KotlinSubClass: KotlinClass() {
    @MyObjcName("objCProperty")
    override var kotlinProperty: Int = 1
    @MyObjcName(swiftName = "swiftFunction")
    override fun @receiver:MyObjcName("objCReceiver") Int.kotlinFunction(
    @MyObjcName("objCParam") kotlinParam: Int
    ): Int = this + kotlinParam * 2
}

@MyObjcName()
val invalidObjCName: Int = 0

// MODULE: platform()()(common)
// FILE: kotlin.kt
package kotlin.native

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class ObjCName(val name: String = "", val swiftName: String = "", val exact: Boolean = false)

// FILE: platform.kt
actual typealias MyObjcName = kotlin.native.ObjCName
