interface Out<out E>

fun <X> id(x: Out<X>): Out<X> = TODO()

fun <F : Any> foo(computable: Out<F?>)

fun <T : Any> bar(computable: Out<T?>) {
    // Should be resolved but fails during inference
    // Failed incorporated constraint: T? <: Any (from T? <: X!! and X!! <: F and F <: Any)
    // Hypothesis: DefinetelyNotNull(T?) = should just remove "?" while it actually gets created
    <!INAPPLICABLE_CANDIDATE!>foo<!>(id(computable))
}
