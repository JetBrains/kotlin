// FIR_IDENTICAL
// ISSUE: KT-66279

fun foo() {}

typealias MyUnit = Unit

// Note that resolution works differently for lambdas passed as function arguments and lambdas assigned to variables,
// thus we need to test both cases.

// ================= Lambdas assigned to a variable =================

val expectedUnitEmptyReturnUnitAndString: () -> Unit = l@ {
    if ("0".hashCode() == 42) return@l
    ""
}

val expectedMyUnitEmptyReturnUnitAndString: () -> Unit = l@ {
    if ("0".hashCode() == 42) return@l
    ""
}

val expectedUnitExplicitReturnUnitAndString: () -> Unit = l@ {
    if ("0".hashCode() == 42) return@l Unit
    ""
}

val expectedUnitExplicitReturnMyUniAndString: () -> Unit = l@ {
    if ("0".hashCode() == 42) return@l MyUnit
    ""
}

val expectedUnitExplicitReturnFooAndString: () -> Unit = l@ {
    if ("0".hashCode() == 42) return@l foo()
    ""
}

val expectedUnitEmptyReturnOnly: () -> Unit = l@ {
    if ("0".hashCode() == 42) return@l
    return@l
}

val expectedUnitEmptyReturnAndExplicitReturnUnit: () -> Unit = l@ {
    if ("0".hashCode() == 42) return@l
    return@l Unit
}

val expectedUnitEmptyReturnAndExplicitReturnFoo: () -> Unit = l@ {
    if ("0".hashCode() == 42) return@l
    return@l foo()
}

val expectedMyUnitEmptyReturnOnly: () -> MyUnit = l@ {
    if ("0".hashCode() == 42) return@l
    return@l
}

val expectedMyUnitEmptyReturnAndExplicitReturnMyUnit: () -> MyUnit = l@ {
    if ("0".hashCode() == 42) return@l
    return@l Unit
}

// ============== Lambdas passed as function argument ===============

fun test() {
    run<Unit> l@ {
        if ("0".hashCode() == 42) return@l
        ""
    }

    run<MyUnit> l@ {
        if ("0".hashCode() == 42) return@l
        ""
    }

    run<Unit> l@ {
        if ("0".hashCode() == 42) return@l Unit
        ""
    }

    run<Unit> l@ {
        if ("0".hashCode() == 42) return@l MyUnit
        ""
    }

    run<Unit> l@ {
        if ("0".hashCode() == 42) return@l foo()
        ""
    }

    run<Unit> l@ {
        if ("0".hashCode() == 42) return@l
        return@l
    }

    run<Unit> l@ {
        if ("0".hashCode() == 42) return@l
        return@l Unit
    }

    run<Unit> l@ {
        if ("0".hashCode() == 42) return@l
        return@l foo()
    }

    run<MyUnit> l@ {
        if ("0".hashCode() == 42) return@l
        return@l
    }

    run<MyUnit> l@ {
        if ("0".hashCode() == 42) return@l
        return@l Unit
    }
}
