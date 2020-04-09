// KT-9277 Unexpected NullPointerException in an invocaton with named arguments

fun box(): String {
    foo(null)

    return "OK"
}

fun foo(x : Int?){
    bar(z = x ?: return, y = x)
}

fun bar(y : Int, z : Int) {}