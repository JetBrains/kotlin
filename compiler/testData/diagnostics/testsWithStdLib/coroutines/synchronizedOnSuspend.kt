// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

<!SYNCHRONIZED_ON_SUSPEND_WARNING!>@Synchronized<!>
suspend fun foo(f: () -> Unit): Unit = f()

fun builder(c: suspend () -> Unit) {}

val a: suspend () -> Unit
    get() = <!SYNCHRONIZED_ON_SUSPEND_WARNING!>@Synchronized<!> {}
val b: suspend () -> Unit = <!SYNCHRONIZED_ON_SUSPEND_WARNING!>@Synchronized<!> {}
val c = builder (<!SYNCHRONIZED_ON_SUSPEND_WARNING!>@Synchronized<!> {})
val d = suspend <!SYNCHRONIZED_ON_SUSPEND_WARNING!>@Synchronized<!> {}
val e = <!WRONG_ANNOTATION_TARGET!>@Synchronized<!> suspend {}

fun test() {
    <!SYNCHRONIZED_ON_SUSPEND_WARNING!>@Synchronized<!>
    suspend fun foo(f: () -> Unit): Unit = f()

    val b: suspend () -> Unit = <!SYNCHRONIZED_ON_SUSPEND_WARNING!>@Synchronized<!> {}
    val c = builder (<!SYNCHRONIZED_ON_SUSPEND_WARNING!>@Synchronized<!> {})
    val d = suspend <!SYNCHRONIZED_ON_SUSPEND_WARNING!>@Synchronized<!> {}
    val e = <!WRONG_ANNOTATION_TARGET!>@Synchronized<!> suspend {}
}
