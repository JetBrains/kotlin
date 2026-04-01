// LANGUAGE: +ValueClasses
// CHECK_BYTECODE_LISTING

value class UnitWrapper(val unit: Unit)
value class NothingWrapper(val nothing: Nothing)

value class UnitWrapper1<T: Unit>(val unit: T)
value class NothingWrapper1<T: Nothing>(val nothing: T)

fun NothingWrapper.wrap() {
    NothingWrapper(this.nothing)
}

fun box(): String {
    val unitWrapper = UnitWrapper(Unit)
    require(unitWrapper == UnitWrapper(Unit))
    require(unitWrapper.unit == Unit)

    val unitWrapper1 = UnitWrapper1(Unit)
    require(unitWrapper1 == UnitWrapper1(Unit))
    require(unitWrapper1.unit == Unit)
    
    return "OK"
}
