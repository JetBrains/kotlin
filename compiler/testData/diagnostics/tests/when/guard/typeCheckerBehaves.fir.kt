// LANGUAGE: +WhenGuards
// WITH_STDLIB
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -USELESS_CAST, -DUPLICATE_LABEL_IN_WHEN

typealias BooleanAlias = Boolean

class InvariantBoxBoolean<T: <!FINAL_UPPER_BOUND!>Boolean<!>>(val value: T)
class InvariantBox<T>(val value: T)

class OuterBoundedBooleanHolder<out T : <!FINAL_UPPER_BOUND!>Boolean<!>>(val value: T) {
    fun getValue(): T {
        return value
    }
}

class InnerBoundedBooleanHolder<in T : <!FINAL_UPPER_BOUND!>Boolean<!>>(val x: Boolean) {
    fun compare(value: T): Boolean {
        return x == value
    }
}

fun capturedTypesBoundedByBoolean(x: Any, outProjectedBox: InvariantBox<out Boolean>, inProjectedBox: InvariantBox<in Boolean>, starBoolean: InvariantBoxBoolean<*>, star: InvariantBox<*>) {
    when (x) {
        is Boolean if <!CONDITION_TYPE_MISMATCH!>inProjectedBox.value<!> -> 10 // [CONDITION_TYPE_MISMATCH] Condition type mismatch: inferred type is 'CapturedType(in kotlin.Boolean)' but 'Boolean' was expected.
        is Boolean if outProjectedBox.value -> 20
        is Boolean if starBoolean.value -> 30
        is Boolean if <!CONDITION_TYPE_MISMATCH!>star.value<!> -> 40 // [CONDITION_TYPE_MISMATCH] Condition type mismatch: inferred type is 'CapturedType(*)' but 'Boolean' was expected.
    }
}

fun <T:<!FINAL_UPPER_BOUND!>Boolean<!>> typeVariablesBoundedByBoolean(x: T) {
    when (x) {
        is Boolean if x -> 250
        is String if x -> 270
        is BooleanAlias if x -> 20
    }
}

fun typeCheckerBehavior(x: Any, y: Any) {
    when (x) {
        is BooleanAlias if y is BooleanAlias && x == y -> 100
        is BooleanAlias if y is BooleanAlias && !x -> 200
        is BooleanAlias if x -> 10
        is BooleanAlias if !x -> 50

        is Boolean if InnerBoundedBooleanHolder<Boolean>(true).compare(x) -> 300
        is Boolean if InnerBoundedBooleanHolder<<!UPPER_BOUND_VIOLATED!>Regex<!>>(<!ARGUMENT_TYPE_MISMATCH!>true<!>).compare(<!ARGUMENT_TYPE_MISMATCH!>x<!>) -> 325
        !is Boolean if InnerBoundedBooleanHolder<Boolean>(false).compare(<!ARGUMENT_TYPE_MISMATCH!>x<!>) -> 350
        is String if InnerBoundedBooleanHolder<Boolean>(true).compare(<!ARGUMENT_TYPE_MISMATCH!>x<!>) -> 400
        is Boolean if OuterBoundedBooleanHolder<Boolean>(true).getValue() -> 500
        is Boolean if <!CONDITION_TYPE_MISMATCH, TYPE_MISMATCH!>OuterBoundedBooleanHolder<<!UPPER_BOUND_VIOLATED!>CharSequence<!>>(<!ARGUMENT_TYPE_MISMATCH!>true<!>).getValue()<!> -> 600
    }
}