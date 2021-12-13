const val add = 1 + 2 + 3 + 4
const val concat = "1" + 2 + 3 + 4
const val divide = 1 / 2.0
const val mul = 10 * 100L

const val toString1 = 1.toString()
const val toString2 = "2".toString()
const val toString3 = '3'.toString()
const val toString4 = 4L.toString()
const val toString5 = 5.0.toString()

const val equals1 = "1" == toString1
const val equals2 = "4" == toString4
const val equals3 = "5.0" == toString5
const val equals4 = mul == 100L
const val equals5 = divide == 0.5

fun box(): String {
    if (add != 10) return "Fail 1"
    if (concat != "1234") return "Fail 2"
    if (divide != 0.5) return "Fail 3"
    if (mul != 1000L) return "Fail 4"

    if (toString1 != "1") return "Fail 5"
    if (toString2 != "2") return "Fail 6"
    if (toString3 != "3") return "Fail 7"
    if (toString4 != "4") return "Fail 8"
    if (toString5 != "5.0") return "Fail 9"

    if (equals1 != true) return "Fail 10"
    if (equals2 != true) return "Fail 11"
    if (equals3 != true) return "Fail 12"
    if (equals4 != false) return "Fail 13"
    if (equals5 != true) return "Fail 14"
    return "OK"
}