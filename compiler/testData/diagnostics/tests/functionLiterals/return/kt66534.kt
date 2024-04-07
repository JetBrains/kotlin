// ISSUE: KT-66534
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
    <!TYPE_MISMATCH!>""<!>
}

fun expectedFlexibleUnitEmptyReturnAndString() {
    A.foo = l@ {
        if ("0".hashCode() == 42) return@l
        ""
    }
}

val expectedNullableUnitEmptyReturnAndExplicitReturnNull: () -> Unit? = l@ {
    if ("0".hashCode() == 42) <!RETURN_TYPE_MISMATCH!>return@l<!>
    return@l <!RETURN_TYPE_MISMATCH!>null<!>
}

fun expectedFlexibleUnitEmptyReturnAndExplicitReturnNull() {
    A.foo = l@ {
        if ("0".hashCode() == 42) return@l
        return@l <!RETURN_TYPE_MISMATCH!>null<!>
    }
}

val expectedNullableUnitExplicitReturnUnitAndString: () -> Unit? = l@ {
    if ("0".hashCode() == 42) return@l Unit
    <!TYPE_MISMATCH!>""<!>
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
    return@l <!TYPE_MISMATCH!>""<!>
}

fun expectedFlexibleUnitExplicitReturnString() {
    A.foo = l@ {
        return@l <!TYPE_MISMATCH!>""<!>
    }
}

val expectedNullableUnitExplicitReturnNull: () -> Unit? = l@ {
    return@l null
}

fun expectedFlexibleUnitExplicitReturnNull() {
    A.foo = l@ {
        return@l null
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
        return@l <!RETURN_TYPE_MISMATCH!>null<!>
    }

    A.run l@ {
        if ("0".hashCode() == 42) return@l
        return@l <!RETURN_TYPE_MISMATCH!>null<!>
    }

    run<Unit?> l@ {
        if ("0".hashCode() == 42) return@l Unit
        <!TYPE_MISMATCH!>""<!>
    }

    A.run l@ {
        ""
    }

    A.run l@ {
        if ("0".hashCode() == 42) return@l Unit
        ""
    }

    run<Unit?> l@ {
        return@l <!TYPE_MISMATCH!>""<!>
    }

    A.run l@ {
        return@l <!TYPE_MISMATCH, TYPE_MISMATCH!>""<!>
    }

    run<Unit?> l@ {
        return@l null
    }

    A.run l@ {
        return@l null
    }
}
