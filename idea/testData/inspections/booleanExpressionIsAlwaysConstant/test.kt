// WITH_RUNTIME


fun foo(a: Int, b: Int) {
    val flag = a > b
    if (flag) {
        val notflag = !flag
        if (notflag)
            print("Hello")
    }
}

fun boo(a: Boolean) {
    val flag = a
    val flag2 = a
    if (flag) {
        if (!flag2)
            print("Hello")
    }
}

fun boo2(a: Boolean) {
    val flag = a
    val flag2 = a
    if (flag && !flag2) {
        print("Hello")
    }
}

fun boo3(a: Boolean) {
    val flag = a
    val flag2 = a
    if (flag && flag2) {
        print("Hello")
    }
}

fun foo1(b: Boolean) {
    if (b)
        return
    if (b)
        print("hello")
}

fun foo2(b: Boolean) {
    if (!b)
        return
    if (b)
        print("hello")
}

fun foo3(b: Boolean) {
    if (b)
        print ("abacaba")
    else
        return
    if (b)
        print("hello")
}

fun foo4(b: Boolean) {
    if (!b)
        print ("abacaba")
    else
        return
    if (b)
        print("hello")
}

fun foo5(a: Boolean, b: Boolean) {
    if (a && b) {
        if (b)
            print("hello")
    } else {
        if (a)
            print("hello")
    }
}

fun foo6(a: Boolean, b: Boolean, c: Boolean) {
    if (a && b && c) {
        if (c)
            print("hello")
    } else {
        if (!b)
            print("hello")
    }
}

fun foo7(a: Boolean, b: Boolean, c: Boolean) {
    if (a || a) {
        print("hello")
    }
}

fun foo1Fp(a: Boolean) {
    val x = if (a) 1 else 0
    val y = if (a) 2 else 3
}

fun foo2Fp(a: Boolean, o: Any): Int {
    when(o) {
        is Int -> if (a) return 1
        is String -> if (a) return 2
    }
    return 3
}

fun foo3FpLambda(ints: Array<Int>, b : Boolean) {
    ints.forEach {
        if (b) return  // nonlocal return from inside lambda directly to the caller of foo()
        print(it)
    }
    if (b)
        print("abacaba")
}




