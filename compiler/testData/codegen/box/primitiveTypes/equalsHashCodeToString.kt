fun box(): String {
    // byte char short int long float double boolean
    val b = 42.toByte()
    val c = 'z'
    val s = 239.toShort()
    val i = -1
    val j = -42L
    val f = 3.14f
    val d = -2.72
    val z = true

    b.equals(b)
    b equals b
    b == b
    b.hashCode()
    b.toString()
    "$b"

    c.equals(c)
    c equals c
    c == c
    c.hashCode()
    c.toString()
    "$c"

    s.equals(s)
    s equals s
    s == s
    s.hashCode()
    s.toString()
    "$s"

    i.equals(i)
    i equals i
    i == i
    i.hashCode()
    i.toString()
    "$i"

    j.equals(j)
    j equals j
    j == j
    j.hashCode()
    j.toString()
    "$j"

    f.equals(f)
    f equals f
    f == f
    f.hashCode()
    f.toString()
    "$f"

    d.equals(d)
    d equals d
    d == d
    d.hashCode()
    d.toString()
    "$d"

    z.equals(z)
    z equals z
    z == z
    z.hashCode()
    z.toString()
    "$z"

    return "OK"
}
