// TARGET_BACKEND: WASM
// ENABLE_TAIL_CALLS
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-JS:2.4
// ^ `-Xwasm-enable-tail-calls` was introduced in 2.5

// Tests for the associative reduction lowering. Each transformed function recurses at a depth
// that would stack-overflow on the host engine if its helper did not use `return_call`.

// The accumulator helper's self-call is emitted as return_call
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=countDown$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=sumTo$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=maskChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=sumEvens$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=repeatStr$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=productChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=andChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=xorChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=longProduct$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=longAndChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=longOrChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=longXorChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=boolAndChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=boolOrChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=boolXorChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=mixedCountDown$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=nestedConcat$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=mixedDirection$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=fib$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=tryMixed$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=deepNestedConcat$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=Counter.count$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=fieldSum$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=countIf$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=sumWhen$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=countComposite$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=sumShifted$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=orderSum$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=countOrFail$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=countOrError$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=countOrElseThrow$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=Bag.count$accum

// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction="Chain.<get-length>$accum"

// The original becomes a single forwarding tail call with no branch left in it
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=countDown
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=if inFunction=countDown

// Every site becomes return_call; a site left as a plain call would lower the count
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=mixedCountDown$accum count=2
// WASM_COUNT_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=sumEvens$accum count=2

// A self-call outside tail position goes to the helper, not through the original
// WASM_CHECK_CALLED_IN_FUNCTION: shouldBeCalled=fib$accum inFunction=fib$accum

// Shapes the lowering must leave alone
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call inFunction=nestedMixedOps
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call inFunction=A.f
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=countGeneric$accum inFunction=countGeneric
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=suffixChain$accum inFunction=suffixChain
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=bothSidesInt$accum inFunction=bothSidesInt
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=rejectedReassoc$accum inFunction=rejectedReassoc
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=render$accum inFunction=render
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=countNamed$accum inFunction=countNamed
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled=sumWide$accum inFunction=sumWide
// WASM_CHECK_NOT_CALLED_IN_FUNCTION: shouldNotBeCalled="count-impl$accum" inFunction="Step$count-impl"
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call inFunction=twoOps
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call inFunction=minusChain
// WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION: instruction=return_call inFunction=doubleSum


// One function per row of the monoid table. The expected values in box() tell a wrong identity element apart.
fun countDown(n: Int): Int {
    if (n == 0) return 0
    return 1 + countDown(n - 1)
}

fun productChain(n: Int): Int {
    if (n == 0) return 1
    return 2 * productChain(n - 1)
}

fun andChain(n: Int, mask: Int): Int {
    if (n == 0) return mask
    return mask and andChain(n - 1, mask)
}

fun maskChain(n: Int, bit: Int): Int {
    if (n == 0) return 0
    return maskChain(n - 1, bit) or bit
}

fun xorChain(n: Int): Int {
    if (n == 0) return 0
    return 1 xor xorChain(n - 1)
}

fun sumTo(n: Long): Long {
    if (n == 0L) return 0L
    return n + sumTo(n - 1L)
}

fun longProduct(n: Int): Long {
    if (n == 0) return 1L
    return 3L * longProduct(n - 1)
}

fun longAndChain(n: Int, mask: Long): Long {
    if (n == 0) return mask
    return mask and longAndChain(n - 1, mask)
}

fun longOrChain(n: Int, bit: Long): Long {
    if (n == 0) return 0L
    return bit or longOrChain(n - 1, bit)
}

fun longXorChain(n: Int): Long {
    if (n == 0) return 0L
    return 1L xor longXorChain(n - 1)
}

fun boolAndChain(n: Int): Boolean {
    if (n == 0) return true
    return true and boolAndChain(n - 1)
}

fun boolOrChain(n: Int): Boolean {
    if (n == 0) return false
    return false or boolOrChain(n - 1)
}

fun boolXorChain(n: Int): Boolean {
    if (n == 0) return false
    return true xor boolXorChain(n - 1)
}

fun repeatStr(s: String, n: Int): String {
    if (n == 0) return ""
    return s + repeatStr(s, n - 1)
}


// Several return sites with the same operator
fun sumEvens(n: Int): Int {
    if (n == 0) return 0
    if (n % 2 == 0) return n + sumEvens(n - 1)
    return 0 + sumEvens(n - 1)
}

// Plain self-calls next to accumulating sites
fun mixedCountDown(n: Int): Int {
    if (n == 0) return 0
    if (n % 2 == 0) return mixedCountDown(n - 1)
    return 1 + mixedCountDown(n - 1)
}

// `a + (b + f())` is reassociated to `(a + b) + f()`, at any depth
fun nestedConcat(n: Int): String {
    if (n == 0) return ""
    return "[" + ("." + nestedConcat(n - 1))
}

fun deepNestedConcat(n: Int): String {
    if (n == 0) return ""
    return "a" + ("b" + ("c" + deepNestedConcat(n - 1)))
}

// Both operands are self-calls; the right one becomes the tail call
fun fib(n: Int): Int {
    if (n < 2) return n
    return fib(n - 1) + fib(n - 2)
}

// A site inside `try` stays as it is; the one outside is transformed
fun tryMixed(n: Int): Int {
    if (n == 0) return 0
    if (n % 2 == 0) {
        try {
            return 1 + tryMixed(n - 1)
        } catch (e: IllegalStateException) {
            return -1
        }
    }
    return 1 + tryMixed(n - 1)
}

// The left-recursive site has an impure operand and keeps its evaluation order; only the right-recursive site is transformed
var tagLog = ""
fun tag(): String {
    tagLog += "t"
    return "x"
}
fun mixedDirection(n: Int): String {
    if (n == 0) return "Z"
    if (n % 2 == 0) return "a" + mixedDirection(n - 1)
    return mixedDirection(n - 1) + tag()
}

// A `var` read is not a pure operand either
var fieldCounter = 0
fun fieldSum(n: Int): Int {
    if (n == 0) {
        fieldCounter = 100
        return 0
    }
    if (n % 2 == 0) return 1 + fieldSum(n - 1)
    return fieldSum(n - 1) + fieldCounter
}

// A compound operand built from a monoid operator is pure
fun sumShifted(n: Int, x: Int): Int {
    if (n == 0) return 0
    return sumShifted(n - 1, x) + (x + 2)
}

// The operand is evaluated before the self-call's arguments, as in the source
var trace = ""
fun mark(tag: String, value: Int): Int {
    trace += tag
    return value
}
fun orderSum(n: Int): Int {
    if (n == 0) return 0
    return mark("o", 1) + orderSum(mark("a", n - 1))
}


// Expression bodies: the return is pushed into the branches of `if` and `when`
fun countIf(n: Int): Int = if (n == 0) 0 else 1 + countIf(n - 1)

fun sumWhen(n: Int): Int = when {
    n == 0 -> 0
    n % 2 == 0 -> n + sumWhen(n - 1)
    else -> sumWhen(n - 1)
}

// A branch whose condition lowers to a composite ending in `true` becomes an else branch with a composite result
fun countComposite(n: Int): Int = when {
    n == 0 -> 0
    (n - 1) is Int -> 1 + countComposite(n - 1)
    else -> 0
}

// Base cases of type Nothing: a `throw` and an inline `error()`
fun countOrFail(n: Int): Int =
    if (n < 0) throw IllegalArgumentException("negative") else if (n == 0) 0 else 1 + countOrFail(n - 1)

fun countOrError(n: Int): Int = when {
    n < 0 -> error("negative")
    n == 0 -> 0
    else -> 1 + countOrError(n - 1)
}

// A `when` with a subject whose `else` branch throws
fun countOrElseThrow(n: Int): Int = when (n) {
    0 -> 0
    in 1..Int.MAX_VALUE -> 1 + countOrElseThrow(n - 1)
    else -> throw IllegalArgumentException("negative")
}


// Dispatch receivers are carried over to the helper
class Counter(val step: Int) {
    fun count(n: Int): Int {
        if (n == 0) return 0
        return step + count(n - 1)
    }
}

// Only the function's own type parameters disqualify it; a generic enclosing class is fine
class Bag<T>(val items: List<T>) {
    fun count(i: Int): Int {
        if (i == items.size) return 0
        return 1 + count(i + 1)
    }
}

// A recursive property getter
class Chain(val next: Chain?) {
    val length: Int
        get() = if (next == null) 0 else 1 + next.length
}


// Different operators in a chain are not reassociated
fun nestedMixedOps(n: Int): Int {
    if (n == 0) return 1
    return 1 + (2 * nestedMixedOps(n - 1))
}

// Sites folding on different sides, or with different operators, reject the whole function
fun bothSidesInt(n: Int): Int {
    if (n == 0) return 0
    if (n % 2 == 0) return 1 + bothSidesInt(n - 1)
    return bothSidesInt(n - 1) + 2
}

fun twoOps(n: Int): Int {
    if (n == 0) return 1
    if (n % 2 == 0) return 2 * twoOps(n - 1)
    return 1 + twoOps(n - 1)
}

fun rejectedReassoc(n: Int): Int {
    if (n == 0) return 0
    if (n % 2 == 0) return 1 + (2 + rejectedReassoc(n - 1))
    return rejectedReassoc(n - 1) + 3
}

// A left-recursive String site would prepend to the growing accumulator, so it is left alone
fun suffixChain(n: Int, s: String): String {
    if (n == 0) return "|"
    return suffixChain(n - 1, s) + (s + "x")
}

// A generic function is left alone
fun <T> countGeneric(list: List<T>, i: Int): Int {
    if (i == list.size) return 0
    return 1 + countGeneric(list, i + 1)
}

// Arguments out of order are evaluated into temporaries in a block around the call,
// so the self-call is not a direct operand of the operator
fun countNamed(n: Int, step: Int): Int = if (n == 0) 0 else step + countNamed(step = step, n = n - 1)

// `Int.plus(Long)` is not in the monoid table: its receiver type differs from its return type
fun sumWide(n: Int): Long = if (n == 0) 0L else n + sumWide(n - 1)

// A value class member moves to a static `count-impl`, but its returns still target the
// original member, so no site matches the function being lowered
value class Step(val v: Int) {
    fun count(n: Int): Int = if (n == 0) 0 else v + count(n - 1)
}

// Operators outside the table: not associative, and floating point
fun minusChain(n: Int): Int {
    if (n == 0) return 0
    return 1 - minusChain(n - 1)
}

fun doubleSum(n: Int): Double {
    if (n == 0) return 0.0
    return 0.1 + doubleSum(n - 1)
}

// `"" + o` calls the user's `toString()`, so the operand is not pure and the site is left alone
var stamp = 0

class Item {
    override fun toString() = stamp.toString()
}

fun render(n: Int, o: Item): String {
    if (n == 0) {
        stamp = 9
        return ""
    }
    return render(n - 1, o) + ("" + o)
}

// An overridable member dispatches its self-call virtually and is left alone
open class A {
    open fun f(n: Int): Int {
        if (n == 0) return 0
        return 1 + f(n - 1)
    }
}

class B : A() {
    override fun f(n: Int): Int = if (n == 2) 100 else super.f(n)
}


fun box(): String {
    if (countDown(1_000_000) != 1_000_000) return "fail countDown"
    if (productChain(20) != 1_048_576) return "fail productChain"
    if (andChain(1_000_000, 0b1010) != 0b1010) return "fail andChain"
    if (maskChain(1_000_000, 0b101) != 0b101) return "fail maskChain"
    if (xorChain(1_000_000) != 0) return "fail xorChain even"
    if (xorChain(999_999) != 1) return "fail xorChain odd"
    if (sumTo(1_000_000L) != 500_000_500_000L) return "fail sumTo"
    if (longProduct(13) != 1_594_323L) return "fail longProduct"
    if (longAndChain(1_000_000, 0xFF00L) != 0xFF00L) return "fail longAndChain"
    if (longOrChain(1_000_000, 0x10L) != 0x10L) return "fail longOrChain"
    if (longXorChain(1_000_000) != 0L) return "fail longXorChain even"
    if (longXorChain(999_999) != 1L) return "fail longXorChain odd"
    if (boolAndChain(1_000_000) != true) return "fail boolAndChain"
    if (boolOrChain(1_000_000) != false) return "fail boolOrChain"
    if (boolXorChain(1_000_000) != false) return "fail boolXorChain even"
    if (boolXorChain(999_999) != true) return "fail boolXorChain odd"
    if (repeatStr("ab", 10_000) != "ab".repeat(10_000)) return "fail repeatStr"

    val expectedEvens = ((2L + 1_000_000L) * 500_000L / 2L).toInt() // 2 + 4 + ... + 1_000_000 with Int wrap-around
    if (sumEvens(1_000_000) != expectedEvens) return "fail sumEvens"
    if (mixedCountDown(1_000_000) != 500_000) return "fail mixedCountDown"
    if (nestedConcat(3) != "[.[.[.") return "fail nestedConcat"
    if (nestedConcat(100_000).length != 200_000) return "fail nestedConcat deep"
    if (deepNestedConcat(2) != "abcabc") return "fail deepNestedConcat: " + deepNestedConcat(2)
    if (deepNestedConcat(10_000).length != 30_000) return "fail deepNestedConcat deep"
    if (fib(20) != 6765) return "fail fib"
    if (tryMixed(1000) != 1000) return "fail tryMixed"
    if (mixedDirection(2) != "aZx") return "fail mixedDirection: " + mixedDirection(2)
    if (fieldSum(2) != 101) return "fail fieldSum: " + fieldSum(2)
    if (sumShifted(1_000_000, 1) != 3_000_000) return "fail sumShifted"
    trace = ""
    if (orderSum(2) != 2) return "fail orderSum"
    if (trace != "oaoa") return "fail orderSum order: " + trace

    if (countIf(1_000_000) != 1_000_000) return "fail countIf"
    if (sumWhen(1_000_000) != expectedEvens) return "fail sumWhen"
    if (countComposite(1_000_000) != 1_000_000) return "fail countComposite"
    if (countOrFail(1_000_000) != 1_000_000) return "fail countOrFail"
    try {
        countOrFail(-1)
        return "fail countOrFail: no exception"
    } catch (e: IllegalArgumentException) {
    }
    if (countOrError(1_000_000) != 1_000_000) return "fail countOrError"
    try {
        countOrError(-1)
        return "fail countOrError: no exception"
    } catch (e: IllegalStateException) {
    }
    if (countOrElseThrow(1_000_000) != 1_000_000) return "fail countOrElseThrow"
    try {
        countOrElseThrow(-1)
        return "fail countOrElseThrow: no exception"
    } catch (e: IllegalArgumentException) {
    }

    if (Counter(2).count(1_000_000) != 2_000_000) return "fail Counter.count"
    if (Bag(listOf("a", "b", "c")).count(0) != 3) return "fail Bag.count"
    var chain = Chain(null)
    repeat(200_000) { chain = Chain(chain) }
    if (chain.length != 200_000) return "fail Chain.length"

    if (nestedMixedOps(2) != 7) return "fail nestedMixedOps: " + nestedMixedOps(2)
    if (bothSidesInt(3) != 5) return "fail bothSidesInt: " + bothSidesInt(3)
    if (twoOps(3) != 5) return "fail twoOps: " + twoOps(3)
    if (rejectedReassoc(3) != 9) return "fail rejectedReassoc: " + rejectedReassoc(3)
    if (suffixChain(2, "a") != "|axax") return "fail suffixChain: " + suffixChain(2, "a")
    if (countGeneric(listOf("a", "b", "c"), 0) != 3) return "fail countGeneric"
    if (countNamed(3, 2) != 6) return "fail countNamed: " + countNamed(3, 2)
    if (sumWide(3) != 6L) return "fail sumWide: " + sumWide(3)
    if (Step(2).count(3) != 6) return "fail Step.count: " + Step(2).count(3)
    if (minusChain(3) != 1) return "fail minusChain: " + minusChain(3)
    if (doubleSum(3) != 0.1 + (0.1 + (0.1 + 0.0))) return "fail doubleSum: " + doubleSum(3)
    stamp = 0
    if (render(2, Item()) != "99") return "fail render: " + render(2, Item())
    if (B().f(4) != 102) return "fail virtual: " + B().f(4)

    return "OK"
}
