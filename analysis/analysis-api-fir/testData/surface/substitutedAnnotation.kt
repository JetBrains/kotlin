// mypack.MyFacadeKt
// MODULE: common
// TARGET_PLATFORM: Common
// WITH_STDLIB
// FILE: myFacade.kt
package mypack

@kotlin.jvm.JvmName("myCustomName")
fun annotatedCommonFunction() {

}

// MODULE: jvm()()(common)
// WITH_STDLIB
// FILE: main.kt

fun placeholder() {

}
