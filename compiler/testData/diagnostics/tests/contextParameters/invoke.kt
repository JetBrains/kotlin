// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

fun String.foo(f: <!UNSUPPORTED_FEATURE!>context(String)<!> () -> Unit = {}) {
    f<!NO_VALUE_FOR_PARAMETER!>()<!>
    f("") // explicit argument should still work
}

fun foo(f: <!UNSUPPORTED_FEATURE!>context(String)<!> () -> Unit = {}) {
    f("") // explicit argument should still work
}

interface Ctx
interface CtxA : Ctx
interface CtxB : Ctx

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(a: <!DEBUG_INFO_MISSING_UNRESOLVED!>CtxA<!>, b: <!DEBUG_INFO_MISSING_UNRESOLVED!>CtxB<!>)<!>
fun bar(f: <!UNSUPPORTED_FEATURE!>context(Ctx)<!> () -> Unit) {
    f<!NO_VALUE_FOR_PARAMETER!>()<!>
    f(<!UNRESOLVED_REFERENCE!>a<!>) // explicit argument should still work
    f(<!UNRESOLVED_REFERENCE!>b<!>) // explicit argument should still work
}

<!CONTEXT_PARAMETERS_UNSUPPORTED!>context(_: <!DEBUG_INFO_MISSING_UNRESOLVED!>String<!>)<!>
fun baz(param: String, f: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> (String) -> Unit){
    with(1) {
        f(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>param)<!>
    }
}

fun <T> context(t: T, f: <!UNSUPPORTED_FEATURE!>context(T)<!> () -> Unit) = f(t)

fun qux(
    f1: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> () -> Unit,
    f2: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> (Boolean) -> Unit,
    f3: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> Boolean.() -> Unit,
    f4: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> String.() -> Unit,
    f5: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> String.(Boolean) -> Unit,
) {
    f1("", 1)
    f2("", 1, true)
    f3("", 1, true)
    f4("", 1, "")
    f5("", 1, "", true)

    f1<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
    f2(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
    f3(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
    true.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f3<!>()
    f4(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>"")<!>
    "".f4<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
    f5("", <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
    "".f5(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>

    with("") {
        with(1) {
            f1("", 1)
            f2("", 1, true)
            f3("", 1, true)
            f4("", 1, "")
            f5("", 1, "", true)

            f1<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
            f2(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
            f3(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
            with(true) {
                f3<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
            }
            true.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>f3<!>()
            f4<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
            f4(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>"")<!>
            "".f4<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
            f5<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
            f5(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
            f5("", <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
            "".f5(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
        }
    }

    context("") {
        with(1) {
            f1("", 1)
            f2("", 1, true)
            f3("", 1, true)
            f4("", 1, "")
            f5("", 1, "", true)

            f1<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
            f2(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
        }
    }

    context("") {
        context(1) {
            f1<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
            f2(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
        }
    }

    with("") {
        f1(<!NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)<!>
        f2(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, <!NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)<!>
        f3(1, true)
        f4(1, "")
        f4(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!><!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)<!>
        f5(1, "", true)
    }

    "".f3(1, true)
}
