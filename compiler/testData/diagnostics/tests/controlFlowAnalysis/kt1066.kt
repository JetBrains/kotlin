//KT-1066 false 'Variable cannot be initialized before declaration'

package kt1066

fun randomDigit() = 0.toChar()

fun foo(excluded: Set<Char>) {
    var digit : Char

    do {
        digit = randomDigit()
//      ^^^^^ here!
    } while (excluded.contains(digit))
}

fun test() {
    var sum : Int = 0
    var first : Int = 1
    var second : Int = 2
    var temp : Int //= 0 // variable 'temp' initializer is redundant

    while (true)
    {
        if (second > 4000000)
            break

        if (second % 2 == 0)
            sum += second

        temp = second
        second = first + second
        first = temp
    }
}
