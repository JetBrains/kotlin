open class C0<X>(val x: X) {}

open class C1<T: Any> protected (val x1: T? = null, var x2: Double, x3: ((Int) -> Int)?) : C0<((Int) -> Int)?>(x3){
    fun bar() {
        val y1 = x1;
        val y2 = x2;
    }
}
class C2 : C1<Int>(1, 2.5, null<caret>) {
    fun foo() {
        var c = C1(2, 3.5, null);
        c = C1(x1 = 2, x2 = 3.5, x3 = null);
    }
}