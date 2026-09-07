// DONT_TARGET_EXACT_BACKEND: JVM_IR
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// Class declared in a default argument value
tailrec fun withClassInDefault(
    x: Int = run {
        class Holder(val v: Int)
        Holder(1).v
    }
): Int {
    return withClassInDefault(x - 1)
}

// Recursive call inside a class in default argument — not a tail call of the function
tailrec fun recursionInDefaultClass(
    x: Int = run {
        class C {
            val v = <!NON_TAIL_RECURSIVE_CALL!>recursionInDefaultClass<!>(0)
        }
        C().v
    }
): Int {
    return recursionInDefaultClass(x - 1)
}

// Object expression in default argument
tailrec fun objectInDefault(
    x: Int = run {
        val obj = object {
            val v = 42
        }
        obj.v
    }
): Int {
    return objectInDefault(x - 1)
}

// Object expression in default argument with recursive call
tailrec fun recursionInDefaultObject(
    x: Int = run {
        val obj = object {
            val v = <!NON_TAIL_RECURSIVE_CALL!>recursionInDefaultObject<!>(0)
        }
        obj.v
    }
): Int {
    return recursionInDefaultObject(x - 1)
}

// Lambda in default argument
tailrec fun lambdaInDefault(
    x: Int = run {
        val f = { 42 }
        f()
    }
): Int {
    return lambdaInDefault(x - 1)
}

// Lambda in default argument with recursive call
tailrec fun recursionInDefaultLambda(
    x: Int = run {
        val f = { <!NON_TAIL_RECURSIVE_CALL!>recursionInDefaultLambda<!>(0) }
        f()
    }
): Int {
    return recursionInDefaultLambda(x - 1)
}

// Multiple parameters with default values containing classes
tailrec fun multipleDefaultsWithClasses(
    x: Int = run {
        class A(val v: Int)
        A(1).v
    },
    y: Int = run {
        class B(val v: Int)
        B(2).v
    }
): Int {
    return multipleDefaultsWithClasses(x - 1, y - 1)
}

// Nested class in default argument
tailrec fun nestedClassInDefault(
    x: Int = run {
        class Outer(val v: Int) {
            inner class Inner {
                fun get() = v
            }
        }
        Outer(1).Inner().get()
    }
): Int {
    return nestedClassInDefault(x - 1)
}

// Enum-like sealed class in default argument
tailrec fun sealedInDefault(
    x: Int = run {
        class Wrapper(val v: Int)
        Wrapper(0).v
    }
): Int {
    return sealedInDefault(x - 1)
}

// Default argument with local function inside run
tailrec fun localFunInDefault(
    x: Int = run {
        fun compute(): Int = 42
        compute()
    }
): Int {
    return localFunInDefault(x - 1)
}

// Default argument with local function that calls the tailrec function
tailrec fun recursionInDefaultLocalFun(
    x: Int = run {
        fun compute(): Int = <!NON_TAIL_RECURSIVE_CALL!>recursionInDefaultLocalFun<!>(0)
        compute()
    }
): Int {
    return recursionInDefaultLocalFun(x - 1)
}

// No tail calls at all, but has class in default argument
<!NO_TAIL_CALLS_FOUND_IN_IR!><!NO_TAIL_CALLS_FOUND!>tailrec<!> fun noTailCallWithClassInDefault(
    x: Int = run {
        class C(val v: Int)
        C(1).v
    }
): Int {
    <!NON_TAIL_RECURSIVE_CALL!>noTailCallWithClassInDefault<!>(x - 1)
    return 0
}<!>

// Combination: local function + class in default argument + tail call in body
tailrec fun combinedLocalFunAndDefault(
    x: Int = run {
        class C(val v: Int)
        C(1).v
    }
): Int {
    fun local() { <!NON_TAIL_RECURSIVE_CALL!>combinedLocalFunAndDefault<!>(x - 1) }
    local()
    return combinedLocalFunAndDefault(x - 1)
}
