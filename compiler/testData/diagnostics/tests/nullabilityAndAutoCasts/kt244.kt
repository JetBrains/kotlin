package kt244


// KT-244 Use dataflow info while resolving variable initializers

fun f(s: String?) {
    if (s != null) {
        <!DEBUG_INFO_AUTOCAST!>s<!>.length  //ok
        var <!UNUSED_VARIABLE!>i<!> = <!DEBUG_INFO_AUTOCAST!>s<!>.length //error: Only safe calls are allowed on a nullable receiver
        System.out.println(<!DEBUG_INFO_AUTOCAST!>s<!>.length) //error
    }
}

// more tests
class A(a: String?) {
    val b = if (a != null) <!DEBUG_INFO_AUTOCAST!>a<!>.length else 1
    {
        if (a != null) {
            val <!UNUSED_VARIABLE!>c<!> = <!DEBUG_INFO_AUTOCAST!>a<!>.length
        }
    }

    val i : Int

    {
        if (a is String) {
            i = <!DEBUG_INFO_AUTOCAST!>a<!>.length
        }
        else {
            i = 3
        }
    }
}
