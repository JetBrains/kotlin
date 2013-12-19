fun <T> array1(vararg a : T) = a

fun main(args : Array<String>) {
    val a = array1("a", "b")
    val b = array1(1, 1)
    join(1)
    join(1, "2")
    join(1, "2", "3")
    join(<!NON_VARARG_SPREAD!>*<!>1, "2")
    join(1, *<!TYPE_MISMATCH!>"2"<!>)
    join(x = 1, a = "2")
    join(x = <!NON_VARARG_SPREAD!>*<!>1, a = *<!TYPE_MISMATCH!>"2"<!>)
    join(x = 1, a = *a)
    join(x = 1, a = *<!TYPE_MISMATCH!>b<!>)
    join(1, *a)
    join(1, *<!TYPE_MISMATCH!>b<!>)
    join(1, *a, "3")
    join(1, *<!TYPE_MISMATCH!>b<!>, "3")
    join(1, "4", *a, "3")
    join(1, "4", *<!TYPE_MISMATCH!>b<!>, "3")
    join(1, "4", *a)
    join(1, "4", *a, *a, "3")
    join(1, "4", *a, *<!TYPE_MISMATCH!>b<!>, "3")
    join(a = *a, x = 1)
    join(a = *<!TYPE_MISMATCH!>b<!>, x = 1)
    join(a = <!TYPE_MISMATCH!>a<!>, x = 1)

    joinG<String>(1, "2")
    joinG<String>(<!NON_VARARG_SPREAD!>*<!>1, "2")
    joinG<String>(1, *<!TYPE_MISMATCH!>"2"<!>)
    joinG<String>(x = 1, a = *a)
    joinG<String>(x = 1, a = "2")
    joinG<String>(x = <!NON_VARARG_SPREAD!>*<!>1, a = *<!TYPE_MISMATCH!>"2"<!>)
    joinG<String>(1, *a)
    joinG<String>(1, *a, "3")
    joinG<String>(1, "4", *a, "3")
    joinG<String>(1, "4", *a)
    joinG<String>(1, "4", *a, *a, "3")
    joinG<String>(a = *a, x = 1)

    joinG(1, "2")
    joinG(<!NON_VARARG_SPREAD!>*<!>1, "2")
    <!TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH!>joinG<!>(1, *<!TYPE_MISMATCH!>"2"<!>)
    joinG(x = 1, a = *a)
    joinG(x = 1, a = "2")
    <!TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH!>joinG<!>(x = <!NON_VARARG_SPREAD!>*<!>1, a = *<!TYPE_MISMATCH!>"2"<!>)
    joinG(1, *a)
    joinG(1, *a, "3")
    joinG(1, "4", *a, "3")
    joinG(1, "4", *a)
    joinG(1, "4", *a, *a, "3")
    joinG(a = *a, x = 1)

    val x1 = joinT(1, "2")
    x1 : String?
    val x2 = joinT(<!NON_VARARG_SPREAD!>*<!>1, "2")
    x2 : String?
    val x6 = joinT(1, *a)
    x6 : String?
    val x7 = joinT(1, *a, "3")
    x7 : String?
    val x8 = joinT(1, "4", *a, "3")
    x8 : String?
    val x9 = joinT(1, "4", *a)
    x9 : String?
    val x10 = joinT(1, "4", *a, *a, "3")
    x10 : String?
    val x11 = joinT(a = *a, x = 1)
    x11 : String?
    val x12 = joinT(x = 1, a = *a)
    x12 : String?
}

fun join(x : Int, vararg a : String) : String {
    val b = StringBuilder(x.toString())
    for (s in a) {
        b.append(s)
    }
    return b.toString()
}

fun <T> joinG(x : Int, vararg a : T) : String {
    val b = StringBuilder(x.toString())
    for (s in a) {
        b.append(s)
    }
    return b.toString()
}

fun <T: Any> joinT(<!UNUSED_PARAMETER!>x<!> : Int, vararg <!UNUSED_PARAMETER!>a<!> : T) : T? {
    return null
}