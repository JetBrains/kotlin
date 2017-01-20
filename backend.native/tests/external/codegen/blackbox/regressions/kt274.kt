// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FULL_JDK

fun box() : String {
    val vector = java.util.Vector<Int>()
    vector.add(1)
    vector.add(2)
    vector.add(3)

    var sum = 0
    for(e in vector.elements()) {
        sum += e
    }
    return if(sum == 6) "OK" else "fail"
}
