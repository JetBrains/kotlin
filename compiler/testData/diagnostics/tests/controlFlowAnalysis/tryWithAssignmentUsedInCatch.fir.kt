fun f() {
    var foo = 1
    try {
        foo = 2
        throw RuntimeException()
    } catch (e: Throwable) {
        foo.hashCode()
    }
    throw Exception()
}

fun g() {
    var foo = 1
    try {
        foo = 2
        f()
    } catch (e: Throwable) {
        foo.hashCode()
    }
}

fun h() {
    try {

    }
    finally {
        var foo = 1
        try {
            foo = 2
            g()
        }
        catch (e: Throwable) {
            foo.hashCode()
        }
    }
}