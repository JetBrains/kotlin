// LANGUAGE: +WhenGuards
// WITH_STDLIB
// DIAGNOSTICS: -SENSELESS_COMPARISON, -USELESS_IS_CHECK, -USELESS_CAST, -DUPLICATE_LABEL_IN_WHEN

sealed class NullableBooleanHolder(val value: Boolean?)
sealed class BooleanHolder(val value: Boolean)
sealed class RangeHolder (val value: IntRange)
object True : BooleanHolder(true)
object False : BooleanHolder(false)


infix fun String.has(x: Char): Boolean {
    return this.contains(x);
}

infix fun String.isLongerThan(x: Int): Boolean {
    return this.length > x;
}

fun simpleBooleanExpressionsInGuards(x: Any) {
    when (x) {
        !is NullableBooleanHolder if x == null -> 1
        !is BooleanHolder if x is Boolean && !x -> 5
        !is Int if x !== null -> 7
        !is String if x in 1..10 -> 8
        is NullableBooleanHolder if x.value == null -> 1
        is NullableBooleanHolder if <!CONDITION_TYPE_MISMATCH!>x.value<!> -> 2
        is NullableBooleanHolder if x.value!! -> 3
        is BooleanHolder if x.value -> 4
        is BooleanHolder if !x.value -> 5
        is String if <!CONDITION_TYPE_MISMATCH!>x<!> -> 6
        is String if x == "" -> 7
        is Int if x in 1..10 -> 8
    }
}

fun complexBooleanExpressionsInGuards(x: Any, y: Boolean?) {
    when (x) {
        is String if y != null && y -> 1
        is String if x isLongerThan 0 && <!UNSAFE_CALL!>!<!>y -> 2
        is String if x == "" || !y!! -> 3
        is String if <!EQUALITY_NOT_APPLICABLE_WARNING!>x == true<!> || false -> 4
        is Boolean if listOf(x, false)[0] -> 45
        !is Boolean if <!CONDITION_TYPE_MISMATCH, TYPE_MISMATCH!>listOf(x, false)[1]<!> -> 47
        is Int if x > 0 && x in 1..10 && y == null -> 50
        is Int if (try { y.toString() } catch (e: NumberFormatException) { null } finally { x.toString() has '!' } != null) -> 60
    }
}

fun inOperatorInGuard(x: Any) {
    when (x) {
        is RangeHolder if x.value.first in 1..10 -> 1
        is RangeHolder if x.value == 1..10 -> 2
        else -> 0
    }
}

fun getRange(a: Int, b: Int): IntRange {
    return a..b
}

fun getInt(x: Int): Int {
    return x
}

fun inOperatorInConditionAndGuard(x: Int) {
    when (x) {
        in 1..10 if x == 1 -> 1
        in 10..20 if x != 10 -> 2
        in 100..200  if x in 110..120 -> 3
        in 110..210  if x !in 110..112 -> 4
        in getRange(300, 400)  if x != 350 -> 4
        in {a: Int, b: Int -> a..b}(350, 390)  if x != 360 -> 4
        in getInt(410)..getInt(500)  if x != 450 -> 4
        in {a: Int -> a}(440)..{a: Int -> a}(540)  if x != 450 -> 4
        in (250 + 260)..(300 + 300)  if x != 550 -> 4
        in if (x % 2 == 0) getInt(600)..getInt(700) else getInt(650) .. getInt(750) if x != 650 -> 4
        !in if (x % 3 == 0) getInt(700)..getInt(800) else getInt(750) .. getInt(850) if x != 700 -> 4
        !in 100..200  if x in 10..20 -> 5
        !in 1..100 if x !in 101..200 -> 6
        else -> 0
    }
}

fun typeCastInGuard(x: Any, y: Boolean?) {
    when(x) {
        is String if y as Boolean -> 10
        !is String if x as Boolean -> 20
        is Boolean if y as? Boolean ?: false -> 30
        !is Boolean if y as? Boolean ?: false -> 40
    }
}

fun whenInGuard(x: Any) {
    when (x) {
        is String if when(x.length) {
            10 if x.contains('!') -> true
            else -> when(x.length) {
                20 if x has '!' -> true
                else -> false
            }
        } -> 10
    }
}

fun infixFunInGuard(x: Any, y: Boolean?) {
    when(x) {
        is String if x has '!' -> x has '?'
        is String if x isLongerThan when (y) {
            true if x has '.' -> 10
            else -> 30
        } && x has '!' -> x has '?'
        is String if x isLongerThan <!ARGUMENT_TYPE_MISMATCH!>"1"<!> && x has '!' -> x has '?'
    }
}