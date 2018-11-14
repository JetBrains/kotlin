package b

fun main() {
    var ints : Array<Int?> = arrayOfNulls<Int>(31)

    ints[0] = 4; ints[11] = 5; ints[2] =7
    for(i in 0..ints.size)
        ints[i<!SYNTAX!><!>
}