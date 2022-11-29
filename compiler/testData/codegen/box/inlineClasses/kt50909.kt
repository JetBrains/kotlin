// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

data class Parent(val child: Parent.Child?) {
    val result =
        if (this.child == null) foo(this.child)
        else "Fail"

    @JvmInline
    value class Child(val value: String)
}

fun foo(x: String?): String =
    x ?: "OK"

fun box(): String =
    Parent(null).result
