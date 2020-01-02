// test
val p1 by Some
val p2 = 1
val p3: Int get() = 3
val p4: Int
    get() { return 1 }
val p5: Int

class OneLine {
    val p1 by Some

    val p2 = 1

    val p3: Int get() = 3

    val p4: Int
        get() { return 1 }

    val p5: Int
}

class TwoLines {
    val p1 by Some


    val p2 = 1


    val p3: Int get() = 3


    val p4: Int
        get() { return 1 }


    val p5: Int
}