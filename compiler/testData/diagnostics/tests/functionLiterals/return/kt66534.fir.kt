// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66534, KT-66954
// WITH_STDLIB

// FILE: A.java
import org.jetbrains.annotations.NotNull;
import kotlin.jvm.functions.Function0;
import kotlin.Unit;

public class A {
    public static @NotNull Function0<Unit> foo;
    public static void run(@NotNull Function0<Unit> foo) {}
}

// FILE: main.kt

// Note that resolution works differently for lambdas passed as function arguments and lambdas assigned to variables,
// thus we need to test both cases.

// ================= Lambdas assigned to a variable =================

val expectedNullableUnitEmptyReturnAndString: () -> Unit? = l@ {
    if ("0".hashCode() == 42) <!RETURN_TYPE_MISMATCH!>return@l<!>
    ""
}

fun expectedFlexibleUnitEmptyReturnAndString() {
    A.foo = l@ {
        if ("0".hashCode() == 42) return@l
        ""
    }
}

val expectedNullableUnitEmptyReturnAndExplicitReturnNull: () -> Unit? = l@ {
    if ("0".hashCode() == 42) <!RETURN_TYPE_MISMATCH!>return@l<!>
    return@l null
}

fun expectedFlexibleUnitEmptyReturnAndExplicitReturnNull() {
    A.foo = l@ {
        if ("0".hashCode() == 42) return@l
        return@l <!NULL_FOR_NONNULL_TYPE!>null<!>
    }
}

val expectedNullableUnitExplicitReturnUnitAndString: () -> Unit? = l@ {
    if ("0".hashCode() == 42) return@l Unit
    <!RETURN_TYPE_MISMATCH!>""<!>
}

fun expectedFlexibleUnitImplicitReturnString() {
    A.foo = l@ {
        ""
    }
}

fun expectedFlexibleUnitExplicitReturnUnitAndString() {
    A.foo = l@ {
        if ("0".hashCode() == 42) return@l Unit
        ""
    }
}

val expectedNullableUnitExplicitReturnString: () -> Unit? = l@ {
    return@l <!RETURN_TYPE_MISMATCH!>""<!>
}

fun expectedFlexibleUnitExplicitReturnString() {
    A.foo = l@ {
        return@l <!RETURN_TYPE_MISMATCH!>""<!>
    }
}

val expectedNullableUnitExplicitReturnNull: () -> Unit? = l@ {
    return@l null
}

fun expectedFlexibleUnitExplicitReturnNull() {
    A.foo = l@ {
        return@l <!NULL_FOR_NONNULL_TYPE!>null<!>
    }
}

fun nullableUnit(): Unit? = null

fun expectedFlexibleUnitImplicitReturnNull() {
    A.foo = l@ {
        return@l <!RETURN_TYPE_MISMATCH!>nullableUnit()<!>
    }
}

// ============== Lambdas passed as function argument ===============

fun test() {
    run<Unit?> l@ {
        if ("0".hashCode() == 42) return@l
        ""
    }

    A.run l@ {
        if ("0".hashCode() == 42) return@l
        ""
    }

    run<Unit?> l@ {
        if ("0".hashCode() == 42) return@l
        return@l <!NULL_FOR_NONNULL_TYPE!>null<!>
    }

    A.run l@ {
        if ("0".hashCode() == 42) return@l
        return@l <!NULL_FOR_NONNULL_TYPE!>null<!>
    }

    run<Unit?> l@ {
        if ("0".hashCode() == 42) return@l Unit
        <!RETURN_TYPE_MISMATCH!>""<!>
    }

    A.run l@ {
        ""
    }

    A.run l@ {
        if ("0".hashCode() == 42) return@l Unit
        ""
    }

    run<Unit?> l@ {
        return@l <!RETURN_TYPE_MISMATCH!>""<!>
    }

    A.run l@ {
        return@l <!RETURN_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>""<!>
    }

    run<Unit?> l@ {
        return@l null
    }

    A.run l@ {
        return@l <!NULL_FOR_NONNULL_TYPE!>null<!>
    }
}
