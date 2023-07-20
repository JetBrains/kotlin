// IGNORE_BACKEND: JVM, JVM_IR

package foo

class C<T>(val pp: T)

val <T: Any?> C<T>.p: String get() = pp?.toString() ?: "O"

val <T: Any> C<T>.p: String get() = pp.toString()

fun box(): String {
    val c1 = C<String?>(null)
    val c2 = C<String>("K")

    return c1.p + c2.p

}
