class A<T> {
    operator fun plus(<warning>x</warning>: T): A<T> = this
    operator fun set(<warning>x</warning>: Int, <warning>y</warning>: T) {}
    operator fun get(<warning>x</warning>: T) = 1
}

fun test(a: A<out CharSequence>) {
    a + <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is String but CapturedType(out CharSequence) was expected">""</error>
    a[1] = <error descr="[TYPE_MISMATCH] Type mismatch: inferred type is String but CapturedType(out CharSequence) was expected">""</error>
    a[<error descr="[TYPE_MISMATCH] Type mismatch: inferred type is String but CapturedType(out CharSequence) was expected">""</error>]
}