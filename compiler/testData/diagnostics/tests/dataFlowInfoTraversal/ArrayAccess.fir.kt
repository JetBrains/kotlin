fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null
    val a = Array<Int>(3, {0})

    if (x != null) bar(a[x]) else bar(a[x])
    bar(a[if (x == null) 0 else x])
    bar(a[x])
    
    "123"[x];
    if (x != null) "123"[x];
}
