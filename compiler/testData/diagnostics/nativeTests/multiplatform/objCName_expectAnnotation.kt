// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyObjcName<!>(val name: String = "", val swiftName: String = "", val exact: Boolean = false)

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>("ObjCClass", "SwiftClass")
open class KotlinClass {
    @<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>("objCProperty")
    open var kotlinProperty: Int = 0
    @<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>(swiftName = "swiftFunction")
    open fun @receiver:<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>("objCReceiver") Int.kotlinFunction(
        @<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>("objCParam") kotlinParam: Int
    ): Int = this + kotlinParam
}

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>("ObjCSubClass", "SwiftSubClass")
class KotlinSubClass: KotlinClass() {
    <!INAPPLICABLE_OBJC_NAME{NATIVE}!>@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>("objCProperty")<!>
    override var kotlinProperty: Int = 1
    <!INAPPLICABLE_OBJC_NAME{NATIVE}!>@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>(swiftName = "swiftFunction")<!>
    override fun <!INAPPLICABLE_OBJC_NAME{NATIVE}!>@receiver:<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>("objCReceiver")<!> Int.kotlinFunction(
        <!INAPPLICABLE_OBJC_NAME{NATIVE}!>@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>("objCParam")<!> kotlinParam: Int
    ): Int = this + kotlinParam * 2
}

<!INVALID_OBJC_NAME{NATIVE}!>@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyObjcName<!>()<!>
val invalidObjCName: Int = 0

// MODULE: platform()()(common)
// FILE: platform.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

actual typealias <!ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE, ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE, ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE!>MyObjcName<!> = kotlin.native.ObjCName
