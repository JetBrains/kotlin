// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

fun noInlineRun(f: (String) -> Unit) {
    f("")
}

inline fun test1(a: context(String)() -> Unit) {
    noInlineRun(<!USAGE_IS_NOT_INLINABLE!>a<!>)
    object {
        fun run() {
            <!NON_LOCAL_RETURN_NOT_ALLOWED!>a<!>("")
        }
    }
}

inline var property1: context(String)() -> Unit
    get() = { }
    set(value) {
        noInlineRun(<!USAGE_IS_NOT_INLINABLE!>value<!>)
        object {
            fun run() {
                <!NON_LOCAL_RETURN_NOT_ALLOWED!>value<!>("")
            }
        }
    }

<!NOTHING_TO_INLINE!>inline<!> fun test2(noinline a: context(String)() -> Unit) {
    noInlineRun(a)
}

inline var property2: context(String)() -> Unit
    get() = { }
    set(noinline value) {
        noInlineRun(value)
    }

inline fun test3(crossinline a: context(String)() -> Unit) {
    object {
        fun run() {
            a("")
        }
    }
}

inline var property3: context(String)() -> Unit
    get() = { }
    set(crossinline value) {
        object {
            fun run() {
                value("")
            }
        }
    }
