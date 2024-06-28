interface Inv<T : CharSequence>

fun <E : CharSequence> id(i: Inv<E>): Inv<E> = i

fun foo1(x: Inv<in String>) {
    val y = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<in kotlin.String>")!>id(x)<!> // Should return Inv<in String>
    bar1(y)
}

fun bar1(i: Inv<in String>) {}

fun foo2(x: Inv<in Nothing>) {
    // Inv<out kotlin.CharSequence> is OK here, because it's still a subtype of Inv<in Nothing>
    val y = <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.CharSequence>")!>id(x)<!>
    bar2(y)
}

fun bar2(i: Inv<in Nothing>) {}
