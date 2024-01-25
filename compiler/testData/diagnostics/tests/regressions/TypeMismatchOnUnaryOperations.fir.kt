// K2: See KT-65342

fun main() {
    val a : Int? = null;
    var v = 1
    val b : String = <!INITIALIZER_TYPE_MISMATCH!>v<!>;
    val f : String = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>a!!<!>;
    val g : String = <!INITIALIZER_TYPE_MISMATCH!>v++<!>;
    val g1 : String = <!INITIALIZER_TYPE_MISMATCH!>++v<!>;
    val h : String = <!INITIALIZER_TYPE_MISMATCH!>v--<!>;
    val h1 : String = <!INITIALIZER_TYPE_MISMATCH!>--v<!>;
    val i : String = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>!true<!>;
    val j : String = <!INITIALIZER_TYPE_MISMATCH!>foo@ true<!>;
    val k : String = <!MULTIPLE_LABELS_ARE_FORBIDDEN!>foo@<!> bar@ true;
    val l : String = <!INITIALIZER_TYPE_MISMATCH!>-1<!>;
    val m : String = <!INITIALIZER_TYPE_MISMATCH!>+1<!>;
}
