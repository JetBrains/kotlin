// !DIAGNOSTICS: -UNUSED_VARIABLE
// KT-3496 Type inference bug on y[""]

class B<T> {
    fun <S> x (y: B<Iterable<S>>) {
        val z: S = y[""] // does not work with [], but works with .get()
    }
    operator fun <S> get(<!UNUSED_PARAMETER!>s<!> : String): S = throw Exception()
}