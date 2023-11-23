// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

suspend fun foo() = 1

suspend fun bar(
        x: Int = 2 + <!UNSUPPORTED!>foo()<!>,
        y: suspend () -> Int = { foo() },
        z: () -> Int = { <!NON_LOCAL_SUSPENSION_POINT!>foo<!>() },
        w: Int = myInline { <!UNSUPPORTED!>foo()<!> },
        q: Int = myInline2 { <!NON_LOCAL_SUSPENSION_POINT!>foo<!>() },
        v: Any? = object {
            fun x() = <!NON_LOCAL_SUSPENSION_POINT!>foo<!>()
            suspend fun y() = foo()
        }
) {}

inline fun myInline(x: () -> Unit) = 1
inline fun myInline2(crossinline x: () -> Unit) = 1
