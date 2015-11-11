fun box(): String {
    val b: Byte = 42
    val c: Char = 'z'
    val s: Short = 239
    val i: Int = -1
    val j: Long = -42L

    b.rangeTo(b)
    b rangeTo b
    b..b
    b.rangeTo(s)
    b rangeTo s
    b..s
    b.rangeTo(i)
    b rangeTo i
    b..i
    b.rangeTo(j)
    b rangeTo j
    b..j

    c.rangeTo(c)
    c rangeTo c
    c..c

    s.rangeTo(b)
    s rangeTo b
    s..b
    s.rangeTo(s)
    s rangeTo s
    s..s
    s.rangeTo(i)
    s rangeTo i
    s..i
    s.rangeTo(j)
    s rangeTo j
    s..j

    i.rangeTo(b)
    i rangeTo b
    i..b
    i.rangeTo(s)
    i rangeTo s
    i..s
    i.rangeTo(i)
    i rangeTo i
    i..i
    i.rangeTo(j)
    i rangeTo j
    i..j

    j.rangeTo(b)
    j rangeTo b
    j..b
    j.rangeTo(s)
    j rangeTo s
    j..s
    j.rangeTo(i)
    j rangeTo i
    j..i
    j.rangeTo(j)
    j rangeTo j
    j..j

    return "OK"
}

/*
fun main(args: Array<String>) {
    val s = "bcsij"
    for (i in s) {
        for (j in s) {
            if ((i == 'c') != (j == 'c')) continue
            println("    $i.rangeTo($j)")
            println("    $i rangeTo $j")
            println("    $i..$j")
        }
        println()
    }
}
*/
