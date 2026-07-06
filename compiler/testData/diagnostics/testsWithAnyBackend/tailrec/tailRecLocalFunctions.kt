// DONT_TARGET_EXACT_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// ===== Local functions =====

// Tail call inside a local function does not count as a tail call of the outer function
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun withLocalFun(x: Int): Int {
    fun local() {
        <!NON_TAIL_RECURSIVE_CALL!>withLocalFun<!>(x - 1)
    }
    local()
    return 0
}<!>

// Tail call of the outer function after a local function declaration
tailrec fun tailCallAfterLocalFun(x: Int): Int {
    fun local() {}
    return tailCallAfterLocalFun(x - 1)
}

// Local tailrec function inside a non-tailrec function
fun outerNonTailrec() {
    tailrec fun localTailrec(x: Int): Int {
        return localTailrec(x - 1)
    }
    localTailrec(10)
}

// Local tailrec function with no tail calls
fun outerWithBadLocalTailrec() {
    <!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun localNoTailCalls(x: Int): Int {
        <!NON_TAIL_RECURSIVE_CALL!>localNoTailCalls<!>(x - 1)
        return 0
    }<!>
    localNoTailCalls(10)
}

// Nested local functions: outer tailrec, inner local calls outer
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun nestedLocalFuns(x: Int): Int {
    fun outer() {
        fun inner() {
            <!NON_TAIL_RECURSIVE_CALL!>nestedLocalFuns<!>(x - 1)
        }
        inner()
    }
    outer()
    return 0
}<!>

// Local function that is itself tailrec, inside a tailrec outer
tailrec fun outerTailrecWithLocalTailrec(x: Int): Int {
    tailrec fun localTailrec(y: Int): Int {
        return localTailrec(y - 1)
    }
    localTailrec(x)
    return outerTailrecWithLocalTailrec(x - 1)
}

// Local function with lambda that calls outer tailrec function
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun localFunWithLambdaCallingOuter(x: Int): Int {
    fun local() {
        run { <!NON_TAIL_RECURSIVE_CALL!>localFunWithLambdaCallingOuter<!>(x - 1) }
    }
    local()
    return 0
}<!>

// Tail call in both local function and outer body
tailrec fun tailCallInBothLocalAndOuter(x: Int): Int {
    fun local() {
        <!NON_TAIL_RECURSIVE_CALL!>tailCallInBothLocalAndOuter<!>(x - 1)
    }
    return tailCallInBothLocalAndOuter(x - 1)
}

// Only a non-tail call in the outer body, local function has the recursive call
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun onlyLocalHasCall(x: Int): Int {
    fun local(): Int {
        return <!NON_TAIL_RECURSIVE_CALL!>onlyLocalHasCall<!>(x - 1)
    }
    return local() + 1
}<!>

// Local function shadows the outer tailrec function name
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun shadowedByLocal(x: Int): Int {
    fun shadowedByLocal(y: Int): Int = y
    return shadowedByLocal(x - 1)
}<!>

// Multiple local functions, one calls outer tailrec
tailrec fun multipleLocalFuns(x: Int): Int {
    fun a() {}
    fun b() { <!NON_TAIL_RECURSIVE_CALL!>multipleLocalFuns<!>(x - 1) }
    fun c() {}
    a(); b(); c()
    return multipleLocalFuns(x - 1)
}

// Local extension function calling outer tailrec
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun localExtensionFun(x: Int): Int {
    fun Int.ext() {
        <!NON_TAIL_RECURSIVE_CALL!>localExtensionFun<!>(this - 1)
    }
    x.ext()
    return 0
}<!>

// Anonymous local function (lambda assigned to val) calling outer tailrec
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun localLambdaVal(x: Int): Int {
    val f = { <!NON_TAIL_RECURSIVE_CALL!>localLambdaVal<!>(x - 1) }
    f()
    return 0
}<!>
