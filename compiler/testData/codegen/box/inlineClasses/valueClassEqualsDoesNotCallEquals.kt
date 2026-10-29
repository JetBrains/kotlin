// ISSUE: KT-90675
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.3,2.4
// `==` on a value class with the generated `equals` must compare the underlying values without boxing and calling `equals`.
// Native has the same optimization, but this detection technique (patching `equals` on the boxed prototype) is JS-only.
// See also compiler/testData/codegen/box/inlineClasses/valueClassEqualsDoesNotAllocate.kt

value class Id(val value: Int)

class Holder(val id: Id)

fun isEqual(holder: Holder, other: Id): Boolean = holder.id == other

var equalsCalls = 0

fun box(): String {
    val boxed: Any = Id(0)
    boxed.asDynamic().constructor.prototype.equals = { _: Any? -> equalsCalls++; false }

    if (!isEqual(Holder(Id(1)), Id(1))) return "Fail: Id(1) != Id(1), equals() was called $equalsCalls times"
    if (isEqual(Holder(Id(1)), Id(2))) return "Fail: Id(1) == Id(2)"
    return if (equalsCalls == 0) "OK" else "Fail: Id == Id called equals() $equalsCalls times"
}
