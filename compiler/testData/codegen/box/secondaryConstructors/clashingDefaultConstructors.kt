open class A(val x: String = "abc", val y: String = "efg") {
    constructor(x: String, y: String, z: Int): this(x, y + "#" + z.toString()) {}
    
    override fun toString() = "$x#$y"
}

class B : A {
    constructor(x: String, y: String, z: Int): super(x, y + z.toString()) {}
    constructor(x: String = "xyz", y: String = "123") : super(x, y)  {}
    constructor(x: Double): super(x.toString()) {}
}

fun box(): String {
    val a1 = A().toString()
    if (a1 != "abc#efg") return "fail1: $a1"

    val a2 = A("hij", "klm", 1).toString()
    if (a2 != "hij#klm#1") return "fail2: $a2"

    val a3 = A(x="xyz").toString()
    if (a3 != "xyz#efg") return "fail3: $a3"

    val b1 = B().toString()
    if (b1 != "xyz#123") return "fail4: $b1"

    val b2 = B("hij", "klm", 2).toString()
    if (b2 != "hij#klm2") return "fail5: $b2"

    val b3 = B(123.0).toString()
    if (b3 != "123.0#efg") return "fail6: $b3"

    val b4 = B(x="test").toString()
    if (b4 != "test#123") return "fail7: $b4"
    return "OK"
}
