// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

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
    <!INAPPLICABLE_OBJC_NAME!>@MyObjcName("objCProperty")<!>
    override var kotlinProperty: Int = 1
    <!INAPPLICABLE_OBJC_NAME!>@MyObjcName(swiftName = "swiftFunction")<!>
    override fun <!INAPPLICABLE_OBJC_NAME!>@receiver:MyObjcName("objCReceiver")<!> Int.kotlinFunction(
        <!INAPPLICABLE_OBJC_NAME!>@MyObjcName("objCParam")<!> kotlinParam: Int
    ): Int = this + kotlinParam * 2
}

<!INVALID_OBJC_NAME!>@MyObjcName()<!>
val invalidObjCName: Int = 0

// MODULE: platform()()(common)
// FILE: platform.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

actual typealias MyObjcName = kotlin.native.ObjCName
