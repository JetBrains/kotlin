class Pair<out X, out Y>(val x: X, val y: Y)

class Inv<T>(val x: T)

fun foo(): Inv<Pair<CharSequence, CharSequence>> = Inv(Pair("O", "K"))

fun bar(inv: Inv<Pair<CharSequence, CharSequence>>) = inv.x.x.toString() + inv.x.y

fun box(): String {
    return JavaClass.test();
}
