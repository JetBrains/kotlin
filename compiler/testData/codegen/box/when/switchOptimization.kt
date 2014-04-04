fun foo1(x : Int) : Int {
    return when (x % 10) {
        1,2,3 -> 1
        4,5,6 -> 2
        7,8,9 -> 3
        else -> 4
    }
}

fun foo2(x : Int) : Int {
    when (x % 10) {
        1,2,3 -> return 1
        4,5,6 -> return 2
        7,8,9 -> return 3
    }

    return 4
}

fun foo3(x : Int) : Int {
    when (x % 10) {
        1 -> return 1
        2 -> return 1
        3 -> return 1
        4 -> return 2
        5 -> return 2
        6 -> return 2
        7 -> return 3
        8 -> return 3
        9 -> return 3
    }

    return 4
}

fun foo4(x : Int) : Int {
    return when (x % 10) {
        1 -> 1
        2 -> 1
        3 -> 1
        4 -> 2
        5 -> 2
        6 -> 2
        7 -> 3
        8 -> 3
        9 -> 3
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
                     test({x -> foo2(x)}, "foo2") +
                     test({x -> foo3(x)}, "foo3") +
                     test({x -> foo4(x)}, "foo4")
    if (testResult != "") return testResult
    return "OK"
}

