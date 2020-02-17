fun <T, R> List<T>.myMap(block: (T) -> R): List<R> = null!!

fun test_1() {
    class Data(val x: Int)
    val datas: List<Data> = null!!
    val xs = datas.<!INAPPLICABLE_CANDIDATE!>myMap<!>(<!UNRESOLVED_REFERENCE!>Data::x<!>)
}
