fun box(): String {
    // byte char short int long float double
    val b = 42.toByte()
    val c = 'z'
    val s = 239.toShort()
    val i = -1
    val j = -42L
    val f = 3.14f
    val d = -2.72

    b.rangeTo(b)
    b rangeTo b
    b..b
    b.rangeTo(c)
    b rangeTo c
    b..c
    b.rangeTo(s)
    b rangeTo s
    b..s
    b.rangeTo(i)
    b rangeTo i
    b..i
    b.rangeTo(j)
    b rangeTo j
    b..j
    b.rangeTo(f)
    b rangeTo f
    b..f
    b.rangeTo(d)
    b rangeTo d
    b..d

    c.rangeTo(c)
    c rangeTo c
    c..c

    s.rangeTo(b)
    s rangeTo b
    s..b
    s.rangeTo(c)
    s rangeTo c
    s..c
    s.rangeTo(s)
    s rangeTo s
    s..s
    s.rangeTo(i)
    s rangeTo i
    s..i
    s.rangeTo(j)
    s rangeTo j
    s..j
    s.rangeTo(f)
    s rangeTo f
    s..f
    s.rangeTo(d)
    s rangeTo d
    s..d

    i.rangeTo(b)
    i rangeTo b
    i..b
    i.rangeTo(c)
    i rangeTo c
    i..c
    i.rangeTo(s)
    i rangeTo s
    i..s
    i.rangeTo(i)
    i rangeTo i
    i..i
    i.rangeTo(j)
    i rangeTo j
    i..j
    i.rangeTo(f)
    i rangeTo f
    i..f
    i.rangeTo(d)
    i rangeTo d
    i..d

    j.rangeTo(b)
    j rangeTo b
    j..b
    j.rangeTo(c)
    j rangeTo c
    j..c
    j.rangeTo(s)
    j rangeTo s
    j..s
    j.rangeTo(i)
    j rangeTo i
    j..i
    j.rangeTo(j)
    j rangeTo j
    j..j
    j.rangeTo(f)
    j rangeTo f
    j..f
    j.rangeTo(d)
    j rangeTo d
    j..d

    f.rangeTo(b)
    f rangeTo b
    f..b
    f.rangeTo(c)
    f rangeTo c
    f..c
    f.rangeTo(s)
    f rangeTo s
    f..s
    f.rangeTo(i)
    f rangeTo i
    f..i
    f.rangeTo(j)
    f rangeTo j
    f..j
    f.rangeTo(f)
    f rangeTo f
    f..f
    f.rangeTo(d)
    f rangeTo d
    f..d

    d.rangeTo(b)
    d rangeTo b
    d..b
    d.rangeTo(c)
    d rangeTo c
    d..c
    d.rangeTo(s)
    d rangeTo s
    d..s
    d.rangeTo(i)
    d rangeTo i
    d..i
    d.rangeTo(j)
    d rangeTo j
    d..j
    d.rangeTo(f)
    d rangeTo f
    d..f
    d.rangeTo(d)
    d rangeTo d
    d..d

    return "OK"
}

/*
fun main(args: Array<String>) {
    val s = "bcsijfd"
    for (i in s) {
        for (j in s) {
            if (i == 'c' && j != 'c') continue
            println("    $i.rangeTo($j)")
            println("    $i rangeTo $j")
            println("    $i..$j")
        }
        println()
    }
}
*/
