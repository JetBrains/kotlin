// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

class Wrapper<T> (val x: T)

inline fun <T, R> Wrapper<T>.unwrap(validator: (T) -> R): R = validator(x)

fun <T> select(x: T) {}

class Foo<W>(y: Wrapper<List<W>>) {
    fun <T> MutableCollection<T>.foo(x: T) {}
    fun <T> MutableCollection<T>.foo(x: Iterable<T>) {}

    init {
        /*
         * Before the fix, `foo` can't be disambiguated because the lambda is alanyzed in full mode,
         * not in partial as before the inroducing new posponed arguments analysis.
         *
         * It happens due to the lack of rerun stages after fixation variables
         * (stage 5 – see `fixVariablesOrReportNotEnoughInformation` in `KotlinConstraintSystemCompleter.kt`).
         * Rerun is need as fixation of variables can be make lambda available for analysis.
         *
         * TODO: add tests with lambdas, which can't be analyzed in partial mode, but if can, the code will be successfully resolved.
         */
        ArrayList<W>().foo(y.unwrap { it })
    }
}
