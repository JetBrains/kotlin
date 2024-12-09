// LANGUAGE: +ValhallaValueClasses
// IGNORE_BACKEND_K1: ANY
// ENABLE_JVM_PREVIEW
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_DEXING
// CHECK_BYTECODE_LISTING

value class UnitWrapper(val unit: Unit)
value class NothingWrapper(val nothing: Nothing)

fun NothingWrapper.wrap() {
    NothingWrapper(this.nothing)
}

fun box(): String {
    val unitWrapper = UnitWrapper(Unit)
    require(unitWrapper == UnitWrapper(Unit))
    require(unitWrapper.unit == Unit)
    
    return "OK"
}
