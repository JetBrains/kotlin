// ISSUE: KT-54764

interface A<out T : B<F, E>, F, out E : B<T, F>> {
    fun copy(t: @UnsafeVariance T, f: F, e: @UnsafeVariance E): A<T, F, E>
}

interface B<out X, out Y>

fun foo(a: A<*, String, B<*, String>>, b1: B<*, String>, b2: B<*, *>, b3: B<String, B<*, String>>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("A<B<*, *>, kotlin.String, B<kotlin.Any?, kotlin.String>>")!>a.copy(b2, "", b1)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("A<B<*, *>, kotlin.String, B<kotlin.Any?, kotlin.String>>")!>a.copy(b1, "", b1)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("A<B<*, *>, kotlin.String, B<kotlin.Any?, kotlin.String>>")!>a.copy(b3, "", b1)<!>
}
