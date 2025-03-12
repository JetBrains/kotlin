// IGNORE_FE10
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: Common.kt

expect class ExpectClass

typealias ExpectClassCommonAlias = ExpectClass 

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM

// FILE: Jvm.kt

class JvmClass

actual typealias ExpectClass = JvmClass

fun <T> withGeneric() {}

fun usage(a: Any): <caret_return>ExpectClassCommonAlias {
    
    if (a is <caret_instanceCheck>ExpectClassCommonAlias) {

        withGeneric<<caret_typeArg>ExpectClassCommonAlias>()

    }
}
    
