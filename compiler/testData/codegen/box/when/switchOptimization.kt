
fun foo1(x : Int) : Int {
    return when ((x % 10)*100) {
        700,800,900 -> 3
        100,200,300 -> 1
        400,500,600 -> 2
        else -> 4
    }
}

fun foo2(x : Short) : Int {
    when ((x % 10).toShort()) {
        1.toShort(),2.toShort(),3.toShort() -> return 1
        7.toShort(),8.toShort(),9.toShort() -> return 3
        4.toShort(),5.toShort(),6.toShort() -> return 2
    }

    return 4
}

fun foo3(x : Char) : Int {

    when (x) {
        '5' -> return 2
        '6' -> return 2
        '7' -> return 3
        '8' -> return 3
        '9' -> return 3
        '1' -> return 1
        '2' -> return 1
        '3' -> return 1
        '4' -> return 2
    }

    return 4
}

fun foo4(x : Byte) : Int {
    return when ((x % 10).toByte()) {
        1.toByte() -> 1
        2.toByte() -> 1
        3.toByte() -> 1
        4.toByte() -> 2
        5.toByte() -> 2
        6.toByte() -> 2
        7.toByte() -> 3
        8.toByte() -> 3
        9.toByte() -> 3
        else -> 4
    }
}

fun test(foo : (Int) -> Int, name : String) : String {
    val foo0 = foo(10)

    var result = if (foo0 == 4) "" else "$name[10/$foo0/4]"

    for (i in 11..19) {
        val shouldBe = (i-11) / 3 + 1
        val fooI = foo(i)
        if (fooI != shouldBe) {
            result += "$name[$i/$fooI/$shouldBe]"
        }
    }

    return result
}

fun box(): String {
    val testResult = test({x -> foo1(x)}, "foo1") +
                     test({x -> foo2(x.toShort())}, "foo2") +
                     test({x -> foo3((x % 10 + '0'.toInt()).toChar())}, "foo3") +
                     test({x -> foo4(x.toByte())}, "foo4")
    if (testResult != "") return testResult
    return "OK"
}

