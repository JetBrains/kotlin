// WITH_STDLIB

// ===== Original test: forEach with non-local return and capture =====

fun listOfFactor(number: Int): List<Int> {
    <!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun listOfFactor(number: Int, acc: List<Int>): List<Int> {
        (2..number).forEach {
            if (number % it == 0) return listOfFactor(number / it, acc + it)
        }
        return acc
    }<!>
    return listOfFactor(number, emptyList())
}

// ===== run {} with non-local return capturing parameter =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun countDownRun(n: Int, acc: StringBuilder): String {
    if (n <= 0) return acc.toString()
    run {
        acc.append(n)
        return countDownRun(n - 1, acc)
    }
}<!>

// ===== let {} with non-local return capturing parameter =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun countDownLet(n: Int, acc: StringBuilder): String {
    if (n <= 0) return acc.toString()
    n.let {
        acc.append(it)
        return countDownLet(it - 1, acc)
    }
}<!>

// ===== also {} with non-local return capturing parameter =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun countDownAlso(n: Int, acc: StringBuilder): String {
    if (n <= 0) return acc.toString()
    n.also {
        acc.append(it)
        return countDownAlso(it - 1, acc)
    }
}<!>

// ===== Nested inline lambdas with captures =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun nestedInlineLambdas(n: Int, acc: Int): Int {
    if (n <= 0) return acc
    run {
        run {
            return nestedInlineLambdas(n - 1, acc + n)
        }
    }
}<!>

// ===== Conditional non-local return inside inline lambda =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun conditionalReturnInLambda(n: Int, acc: Int): Int {
    run {
        if (n <= 0) return acc
        return conditionalReturnInLambda(n - 1, acc + n)
    }
}<!>

// ===== forEach with index-based capture =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun findFirst(items: List<Int>, index: Int): Int {
    if (index >= items.size) return -1
    items.subList(index, items.size).forEach {
        if (it > 10) return it
        return findFirst(items, index + 1)
    }
    return -1
}<!>

// ===== Multiple captured parameters mutated between calls =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun multiCapture(a: Int, b: Int, c: String): String {
    if (a <= 0) return "$c:$b"
    run {
        return multiCapture(a - 1, b + a, c + a)
    }
}<!>

// ===== when expression inside inline lambda with non-local return =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun whenInRun(n: Int, acc: Int): Int {
    run {
        when {
            n <= 0 -> return acc
            n % 2 == 0 -> return whenInRun(n - 1, acc + n)
            else -> return whenInRun(n - 1, acc)
        }
    }
}<!>

// ===== Inline lambda with capture in local tailrec function =====

fun localTailrecWithCapture(items: List<Int>): Int {
    <!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun helper(index: Int, acc: Int): Int {
        if (index >= items.size) return acc
        items.subList(index, items.size).forEach {
            return helper(index + 1, acc + it)
        }
        return acc
    }<!>
    return helper(0, 0)
}

// ===== Non-local return from nested let/run chain =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun chainedInline(n: Int, acc: Int): Int {
    if (n <= 0) return acc
    n.let { x ->
        run {
            return chainedInline(x - 1, acc + x)
        }
    }
}<!>

// ===== Inline lambda with early return before recursive call =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun earlyReturnInLambda(n: Int): String {
    run {
        if (n < 0) return "negative"
        if (n == 0) return "zero"
        return earlyReturnInLambda(n - 1)
    }
}<!>

// ===== Large iteration count to verify actual tail-call optimization (no StackOverflow) =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun deepRecursionInRun(n: Int): Int {
    if (n <= 0) return 0
    run {
        return deepRecursionInRun(n - 1)
    }
}<!>

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun deepRecursionInForEach(n: Int): Int {
    if (n <= 0) return 0
    listOf(n).forEach {
        return deepRecursionInForEach(it - 1)
    }
    return 0
}<!>

// ===== Inline lambda capturing mutable accumulator (StringBuilder) =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun buildStringTailrec(n: Int, sb: StringBuilder): String {
    if (n <= 0) return sb.toString()
    run {
        sb.append(if (n % 2 == 0) "E" else "O")
        return buildStringTailrec(n - 1, sb)
    }
}<!>

// ===== Multiple non-local returns in different branches of inline lambda =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun branchingInLambda(n: Int, acc: Int): Int {
    run {
        if (n <= 0) return acc
        if (n % 3 == 0) return branchingInLambda(n - 1, acc + 3)
        if (n % 3 == 1) return branchingInLambda(n - 1, acc + 1)
        return branchingInLambda(n - 1, acc + 2)
    }
}<!>

// ===== forEach on range with non-local return =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun forEachOnRange(n: Int, acc: Int): Int {
    if (n <= 0) return acc
    (1..1).forEach {
        return forEachOnRange(n - 1, acc + n)
    }
    return acc
}<!>

// ===== Inline lambda with captured variable modified before recursive call =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun capturedVarModified(n: Int, list: MutableList<Int>): List<Int> {
    if (n <= 0) return list
    run {
        list.add(n)
        return capturedVarModified(n - 1, list)
    }
}<!>

// ===== if-else inside run with non-local returns in both branches =====

<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec fun ifElseInRun(n: Int, acc: Int): Int {
    run {
        if (n <= 0)
            return acc
        else
            return ifElseInRun(n - 1, acc + n)
    }
}<!>

fun box(): String {
    // Original test
    val factors = listOfFactor(60)
    if (factors.size != 4) return "Fail listOfFactor: $factors"

    // run with capture
    val runResult = countDownRun(5, StringBuilder())
    if (runResult != "54321") return "Fail countDownRun: $runResult"

    // let with capture
    val letResult = countDownLet(5, StringBuilder())
    if (letResult != "54321") return "Fail countDownLet: $letResult"

    // also with capture
    val alsoResult = countDownAlso(5, StringBuilder())
    if (alsoResult != "54321") return "Fail countDownAlso: $alsoResult"

    // nested inline lambdas
    val nestedResult = nestedInlineLambdas(5, 0)
    if (nestedResult != 15) return "Fail nestedInlineLambdas: $nestedResult"

    // conditional return in lambda
    val condResult = conditionalReturnInLambda(5, 0)
    if (condResult != 15) return "Fail conditionalReturnInLambda: $condResult"

    // forEach with index
    val findResult = findFirst(listOf(1, 2, 20, 3), 0)
    if (findResult != 20) return "Fail findFirst: $findResult"

    // multiple captures
    val multiResult = multiCapture(3, 0, "")
    if (multiResult != "321:6") return "Fail multiCapture: $multiResult"

    // when in run
    val whenResult = whenInRun(6, 0)
    if (whenResult != 12) return "Fail whenInRun: $whenResult" // 6+4+2 = 12

    // local tailrec with capture
    val localResult = localTailrecWithCapture(listOf(1, 2, 3, 4))
    if (localResult != 10) return "Fail localTailrecWithCapture: $localResult"

    // chained inline
    val chainResult = chainedInline(5, 0)
    if (chainResult != 15) return "Fail chainedInline: $chainResult"

    // early return in lambda
    if (earlyReturnInLambda(-1) != "negative") return "Fail earlyReturnInLambda negative"
    if (earlyReturnInLambda(0) != "zero") return "Fail earlyReturnInLambda zero"
    if (earlyReturnInLambda(3) != "zero") return "Fail earlyReturnInLambda 3"

    // deep recursion — verifies actual TCO (would StackOverflow without it)
    deepRecursionInRun(100000)
    deepRecursionInForEach(100000)

    // buildStringTailrec with capture
    val bsResult = buildStringTailrec(4, StringBuilder())
    if (bsResult != "EOEO") return "Fail buildStringTailrec: $bsResult"

    // branching in lambda
    val branchResult = branchingInLambda(6, 0)
    if (branchResult != 12) return "Fail branchingInLambda: $branchResult" // 3+1+2+3+1+2 = 12

    // forEach on range
    val rangeResult = forEachOnRange(5, 0)
    if (rangeResult != 15) return "Fail forEachOnRange: $rangeResult"

    // captured var modified
    val capturedResult = capturedVarModified(5, mutableListOf())
    if (capturedResult != listOf(5, 4, 3, 2, 1)) return "Fail capturedVarModified: $capturedResult"

    // if-else in run
    val ifElseResult = ifElseInRun(5, 0)
    if (ifElseResult != 15) return "Fail ifElseInRun: $ifElseResult"

    return "OK"
}
