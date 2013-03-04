package b

trait A<T>

fun infer<T>(a: A<T>) : T {}

fun foo(nothing: Nothing?) {
    val i = infer(nothing)
}
