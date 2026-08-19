// LANGUAGE: +FullValueClasses
// TARGET_PLATFORM: JVM

value class FullValueClass(val value: Int) {
    fun mem<caret>berFunction() {}
}
