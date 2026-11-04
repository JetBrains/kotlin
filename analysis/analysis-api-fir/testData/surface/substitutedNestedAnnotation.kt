// mypack.MyFacadeKt
// MODULE: common
// TARGET_PLATFORM: Common
// WITH_STDLIB
// FILE: myFacade.kt
package mypack

@Deprecated("Use bar()", ReplaceWith("bar()"))
fun annotatedCommonFunction() {

}

// MODULE: jvm()()(common)
// WITH_STDLIB
// FILE: main.kt

fun placeholder() {

}
