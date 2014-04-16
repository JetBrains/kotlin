class A

fun A.plus(a: Int): Int {
    return this <lineMarker descr="Recursive call on plus"></lineMarker>+ 12
}