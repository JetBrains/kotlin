// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

@Synchronized
suspend fun foo(f: () -> Unit): Unit = f()

fun builder(c: suspend () -> Unit) {}

val a: suspend () -> Unit
    get() = @Synchronized {}
val b: suspend () -> Unit = @Synchronized {}
val c = builder (@Synchronized {})
val d = suspend @Synchronized {}
val e = <!WRONG_ANNOTATION_TARGET!>@Synchronized<!> suspend {}

fun test() {
    @Synchronized
    suspend fun foo(f: () -> Unit): Unit = f()

    val b: suspend () -> Unit = @Synchronized {}
    val c = builder (@Synchronized {})
    val d = suspend @Synchronized {}
    val e = <!WRONG_ANNOTATION_TARGET!>@Synchronized<!> suspend {}
}
