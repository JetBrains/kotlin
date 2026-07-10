// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87577
class C<T> {
    fun <T> f() {
        when (T) {
        }
    }
}
