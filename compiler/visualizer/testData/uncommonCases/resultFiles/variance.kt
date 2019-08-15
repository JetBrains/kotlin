interface Source<out T> {
    fun nextT(): T
}

fun demo(strs: Source<String>) {
//      Source<Any>            demo.strs: Source<String>
//      │                      │
    val objects: Source<Any> = strs
}

interface Comparable<in T> {
    operator fun compareTo(other: T): Int
}

fun demo(x: Comparable<Number>) {
//  demo.x: Comparable<Number>
//  │ fun (Comparable<Number>).compareTo(Number): Int
//  │ │         Double
//  │ │         │
    x.compareTo(1.0)
//      Comparable<Double>      demo.x: Comparable<Number>
//      │                       │
    val y: Comparable<Double> = x
}
