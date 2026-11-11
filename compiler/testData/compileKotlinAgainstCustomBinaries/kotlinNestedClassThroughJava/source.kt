// The default argument of `ok` is only known from the Kotlin metadata of `Outer.Nested`, found through the signature of a Java method.
import lib.JavaUser
import lib.Outer

fun test(): String = JavaUser.nested().ok() + Outer.Nested().ok()
