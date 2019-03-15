open class C0(val x: Any?) {}

open class C1(var _x1: Int = 1, _x2: Float?, val _x3: ((Int) -> Int)?) : C0(_x3){
    fun bar() {
        val y1 = _x1;
        val y2 = _x2;
    }
}
class C2 : C1(1, 2.5, null<caret>) {
    fun foo() {
        var c = C1(2, 3.5, null);
        c = C1(_x1 = 2, _x2 = 3.5, _x3 = null);
    }
}
