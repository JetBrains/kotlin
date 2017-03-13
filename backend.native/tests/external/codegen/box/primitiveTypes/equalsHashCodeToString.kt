fun box(): String {
    val b: Byte = 42
    val c: Char = 'z'
    val s: Short = 239
    val i: Int = -1
    val j: Long = -42L
    val f: Float = 3.14f
    val d: Double = -2.72
    val z: Boolean = true

    b.equals(b)
    b == b
    b.hashCode()
    b.toString()
    "$b"

    c.equals(c)
    c == c
    c.hashCode()
    c.toString()
    "$c"

    s.equals(s)
    s == s
    s.hashCode()
    s.toString()
    "$s"

    i.equals(i)
    i == i
    i.hashCode()
    i.toString()
    "$i"

    j.equals(j)
    j == j
    j.hashCode()
    j.toString()
    "$j"

    f.equals(f)
    f == f
    f.hashCode()
    f.toString()
    "$f"

    d.equals(d)
    d == d
    d.hashCode()
    d.toString()
    "$d"

    z.equals(z)
    z == z
    z.hashCode()
    z.toString()
    "$z"

    return "OK"
}
