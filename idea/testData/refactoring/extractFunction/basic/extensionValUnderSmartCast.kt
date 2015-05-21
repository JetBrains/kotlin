// SUGGESTED_NAMES: i, getKm
// PARAM_TYPES: A
// PARAM_DESCRIPTOR: val a: A defined in test
class A {

}

val A.meters: Int? get() = 1

fun test() {
    val a = A()
    if (a.meters == null) return
    val km = <selection>a.meters / 10</selection>
}