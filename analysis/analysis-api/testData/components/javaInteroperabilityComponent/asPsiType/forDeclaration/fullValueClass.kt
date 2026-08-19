// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: JVM

package pack

value class FullValueClass(val value: Int)

fun cons<caret>ume(): FullValueClass {}
