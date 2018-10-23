import kotlin.contracts.*

inline fun <T> funWithExactlyOnceCallsInPlace(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

inline fun funWithExactlyOnceCallsInPlace(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    block()
}

inline fun <T> funWithAtLeastOnceCallsInPlace(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }
    block()
    return block()
}

inline fun funWithAtLeastOnceCallsInPlace(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }
    block()
    block()
}

inline fun funWithAtMostOnceCallsInPlace(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
}

inline fun funWithUnknownCallsInPlace(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.UNKNOWN) }
    block()
}

fun funWithReturns(cond: Boolean) {
    contract { returns() implies (cond) }
    if (!cond) throw Exception()
}

fun funWithReturnsAndInvertCondition(cond: Boolean) {
    contract { returns() implies (!cond) }
    if (cond) throw Exception()
}

fun funWithReturnsAndTypeCheck(value_1: Any?) {
    contract { returns() implies (value_1 is String) }
    if (value_1 !is String) throw Exception()
}

fun funWithReturnsAndInvertTypeCheck(value_1: Any?) {
    contract { returns() implies (value_1 !is String) }
    if (value_1 is String) throw Exception()
}

fun funWithReturnsAndNotNullCheck(value_1: Any?) {
    contract { returns() implies (value_1 != null) }
    if (value_1 == null) throw Exception()
}

fun funWithReturnsAndNullCheck(value_1: Any?) {
    contract { returns() implies (value_1 == null) }
    if (value_1 != null) throw Exception()
}

fun funWithReturnsTrue(cond: Boolean): Boolean {
    contract { returns(true) implies (cond) }
    return cond
}

fun funWithReturnsTrueAndInvertCondition(cond: Boolean): Boolean {
    contract { returns(true) implies (!cond) }
    return !cond
}

fun funWithReturnsTrueAndTypeCheck(value_1: Any?): Boolean {
    contract { returns(true) implies (value_1 is String) }
    return value_1 is String
}

fun funWithReturnsTrueAndInvertTypeCheck(value_1: Any?): Boolean {
    contract { returns(true) implies (value_1 !is String) }
    return value_1 !is String
}

fun funWithReturnsTrueAndNotNullCheck(value_1: Any?): Boolean {
    contract { returns(true) implies (value_1 != null) }
    return value_1 != null
}

fun funWithReturnsTrueAndNullCheck(value_1: Any?): Boolean {
    contract { returns(true) implies (value_1 == null) }
    return value_1 == null
}

fun funWithReturnsFalse(cond: Boolean): Boolean {
    contract { returns(false) implies (cond) }
    return cond
}

fun funWithReturnsFalseAndInvertCondition(cond: Boolean): Boolean {
    contract { returns(false) implies (!cond) }
    return !cond
}

fun funWithReturnsFalseAndTypeCheck(value_1: Any?): Boolean {
    contract { returns(false) implies (value_1 is String) }
    return value_1 is String
}

fun funWithReturnsFalseAndInvertTypeCheck(value_1: Any?): Boolean {
    contract { returns(false) implies (value_1 !is String) }
    return value_1 !is String
}

fun funWithReturnsFalseAndNotNullCheck(value_1: Number?): Boolean {
    contract { returns(false) implies (value_1 != null) }
    return value_1 != null
}

fun funWithReturnsFalseAndNullCheck(value_1: Number?): Boolean {
    contract { returns(false) implies (value_1 == null) }
    return value_1 == null
}

fun funWithReturnsNull(cond: Boolean): Boolean? {
    contract { returns(null) implies (cond) }
    return cond
}

fun funWithReturnsNullAndInvertCondition(cond: Boolean): Boolean? {
    contract { returns(null) implies (!cond) }
    return !cond
}

fun funWithReturnsNullAndTypeCheck(value_1: Any?): Boolean? {
    contract { returns(null) implies (value_1 is String) }
    return value_1 is String
}

fun funWithReturnsNullAndInvertTypeCheck(value_1: Any?): Boolean? {
    contract { returns(null) implies (value_1 !is String) }
    return value_1 !is String
}

fun funWithReturnsNullAndNotNullCheck(value_1: Number?): Boolean? {
    contract { returns(null) implies (value_1 != null) }
    return value_1 != null
}

fun funWithReturnsNullAndNullCheck(value_1: Number?): Boolean? {
    contract { returns(null) implies (value_1 == null) }
    return value_1 == null
}

fun funWithReturnsNotNull(cond: Boolean): Boolean? {
    contract { returnsNotNull() implies (cond) }
    return cond
}

fun funWithReturnsNotNullAndInvertCondition(cond: Boolean): Boolean? {
    contract { returnsNotNull() implies (!cond) }
    return !cond
}

fun funWithReturnsNotNullAndTypeCheck(value_1: Any?): Boolean? {
    contract { returnsNotNull() implies (value_1 is String) }
    return value_1 is String
}

fun funWithReturnsNotNullAndInvertTypeCheck(value_1: Any?): Boolean? {
    contract { returnsNotNull() implies (value_1 !is String) }
    return value_1 !is String
}

fun funWithReturnsNotNullAndNotNullCheck(value_1: Number?): Boolean? {
    contract { returnsNotNull() implies (value_1 != null) }
    return value_1 != null
}

fun funWithReturnsNotNullAndNullCheck(value_1: Number?): Boolean? {
    contract { returnsNotNull() implies (value_1 == null) }
    return value_1 == null
}
