// ISSUE: KT-66277
// FIR_IDENTICAL

fun foo() {}
typealias MyUnit = Unit

// Note that resolution works differently for lambdas passed as function arguments and lambdas assigned to variables,
// thus we need to test both cases.

// ================= Lambdas assigned to a variable =================

val expectedAnyEmptyReturnAndString: () -> Any = l@ {
    if ("0".hashCode() == 42) <!RETURN_TYPE_MISMATCH!>return@l<!>
    ""
}

val expectedAnyExplicitReturnUnitAndString: () -> Any = l@ {
    if ("0".hashCode() == 42) return@l Unit
    ""
}

val expectedAnyExplicitReturnMyUnitAndString: () -> Any = l@ {
    if ("0".hashCode() == 42) return@l MyUnit
    ""
}

val expectedAnyExplicitReturnFooAndString: () -> Any = l@ {
    if ("0".hashCode() == 42) return@l foo()
    ""
}

val expectedAnyEmptyReturnOnly: () -> Any = l@ {
    if ("0".hashCode() == 42) <!RETURN_TYPE_MISMATCH!>return@l<!>
    <!RETURN_TYPE_MISMATCH!>return@l<!>
}

val expectedAnyImplicitAndExplicitReturnUnit: () -> Any = l@ {
    if ("0".hashCode() == 42) <!RETURN_TYPE_MISMATCH!>return@l<!>
    return@l Unit
}

val expectedAnyEmptyReturnAndExplicitReturnMyUnit: () -> Any = l@ {
    if ("0".hashCode() == 42) <!RETURN_TYPE_MISMATCH!>return@l<!>
    return@l MyUnit
}

val expectedAnyEmptyReturnAndExplicitReturnFoo: () -> Any = l@ {
    if ("0".hashCode() == 42) <!RETURN_TYPE_MISMATCH!>return@l<!>
    return@l foo()
}

// ============== Lambdas passed as function argument ===============

fun test() {
    run<Any> l@ {
        if ("0".hashCode() == 42) return@l
        ""
    }

    run<Any> l@ {
        if ("0".hashCode() == 42) return@l Unit
        ""
    }

    run<Any> l@ {
        if ("0".hashCode() == 42) return@l MyUnit
        ""
    }

    run<Any> l@ {
        if ("0".hashCode() == 42) return@l foo()
        ""
    }

    run<Any> l@ {
        if ("0".hashCode() == 42) return@l
        return@l
    }

    run<Any> l@ {
        if ("0".hashCode() == 42) return@l
        return@l Unit
    }

    run<Any> l@ {
        if ("0".hashCode() == 42) return@l
        return@l MyUnit
    }

    run<Any> l@ {
        if ("0".hashCode() == 42) return@l
        return@l foo()
    }
}
