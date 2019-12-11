// !WITH_NEW_INFERENCE
fun main() {
    val a : Int? = null;
    var v = 1
    val b : String = v;
    val f : String = a!!;
    val g : String = v++;
    val g1 : String = ++v;
    val h : String = v--;
    val h1 : String = --v;
    val i : String = !true;
    val j : String = foo@ true;
    val k : String = foo@ bar@ true;
    val l : String = -1;
    val m : String = +1;
}