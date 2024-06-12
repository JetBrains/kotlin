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
        is Boolean <!UNSUPPORTED_FEATURE!>if inProjectedBox.value<!> -> 10 // [CONDITION_TYPE_MISMATCH] Condition type mismatch: inferred type is 'CapturedType(in kotlin.Boolean)' but 'Boolean' was expected.
        is Boolean <!UNSUPPORTED_FEATURE!>if outProjectedBox.value<!> -> 20
        is Boolean <!UNSUPPORTED_FEATURE!>if starBoolean.value<!> -> 30
        is Boolean <!UNSUPPORTED_FEATURE!>if star.value<!> -> 40 // [CONDITION_TYPE_MISMATCH] Condition type mismatch: inferred type is 'CapturedType(*)' but 'Boolean' was expected.
    }
}

fun <T:<!FINAL_UPPER_BOUND!>Boolean<!>> typeVariablesBoundedByBoolean(x: T) {
    when (x) {
        is Boolean <!UNSUPPORTED_FEATURE!>if x<!> -> 250
        is <!INCOMPATIBLE_TYPES!>String<!> <!UNSUPPORTED_FEATURE!>if x<!> -> 270
        is BooleanAlias <!UNSUPPORTED_FEATURE!>if x<!> -> 20
    }
}

fun typeCheckerBehavior(x: Any, y: Any) {
    when (x) {
        is BooleanAlias <!UNSUPPORTED_FEATURE!>if y is BooleanAlias && x == y<!> -> 100
        is BooleanAlias <!UNSUPPORTED_FEATURE!>if y is BooleanAlias && !x<!> -> 200
        is BooleanAlias <!UNSUPPORTED_FEATURE!>if x<!> -> 10
        is BooleanAlias <!UNSUPPORTED_FEATURE!>if !x<!> -> 50

        is Boolean <!UNSUPPORTED_FEATURE!>if InnerBoundedBooleanHolder<Boolean>(true).compare(x)<!> -> 300
        is Boolean <!UNSUPPORTED_FEATURE!>if InnerBoundedBooleanHolder<Regex>(true).compare(x)<!> -> 325
        !is Boolean <!UNSUPPORTED_FEATURE!>if InnerBoundedBooleanHolder<Boolean>(false).compare(x)<!> -> 350
        is String <!UNSUPPORTED_FEATURE!>if InnerBoundedBooleanHolder<Boolean>(true).compare(x)<!> -> 400
        is Boolean <!UNSUPPORTED_FEATURE!>if OuterBoundedBooleanHolder<Boolean>(true).getValue()<!> -> 500
        is Boolean <!UNSUPPORTED_FEATURE!>if OuterBoundedBooleanHolder<CharSequence>(true).getValue()<!> -> 600
    }
}