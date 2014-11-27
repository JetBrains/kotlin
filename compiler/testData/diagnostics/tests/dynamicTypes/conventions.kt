// !MARK_DYNAMIC_CALLS

// MODULE[js]: m1
// FILE: k.kt

fun test(d: dynamic) {
    <!DEBUG_INFO_DYNAMIC!>+<!>d
    <!DEBUG_INFO_DYNAMIC!>-<!>d
    <!DEBUG_INFO_DYNAMIC!>!<!> d


    d <!DEBUG_INFO_DYNAMIC!>+<!> d
    d <!DEBUG_INFO_DYNAMIC!>+<!> 1
    "" + d

    d <!DEBUG_INFO_DYNAMIC!>-<!> d
    d <!DEBUG_INFO_DYNAMIC!>*<!> d
    d <!DEBUG_INFO_DYNAMIC!>/<!> d
    d <!DEBUG_INFO_DYNAMIC!>%<!> d

    d<!DEBUG_INFO_DYNAMIC!>..<!>d

    d <!DEBUG_INFO_DYNAMIC!>and<!> d

    d <!DEBUG_INFO_DYNAMIC!>in<!> d
    d <!DEBUG_INFO_DYNAMIC!>!in<!> d

    1 <!DEBUG_INFO_DYNAMIC!>in<!> d
    1 <!DEBUG_INFO_DYNAMIC!>!in<!> d

    <!DEBUG_INFO_DYNAMIC!>d[1]<!>
    <!DEBUG_INFO_DYNAMIC!>d[1, 2]<!>

    <!DEBUG_INFO_DYNAMIC!>d[1]<!> = 2
    <!DEBUG_INFO_DYNAMIC!>d[1, 2]<!> = 3

    <!DEBUG_INFO_DYNAMIC!>d[1]<!><!DEBUG_INFO_DYNAMIC!>++<!>
    <!DEBUG_INFO_DYNAMIC!>++<!><!DEBUG_INFO_DYNAMIC!>d[1]<!>

    <!DEBUG_INFO_DYNAMIC!>d[1]<!><!DEBUG_INFO_DYNAMIC!>--<!>
    <!DEBUG_INFO_DYNAMIC!>--<!><!DEBUG_INFO_DYNAMIC!>d[1]<!>

//    d()
//    d(1)
//    d(name = 1)
//    d {}

    d == d
    d != d

    d === d
    d !== d

    d <!DEBUG_INFO_DYNAMIC!><<!> d
    d <!DEBUG_INFO_DYNAMIC!><=<!> d
    d <!DEBUG_INFO_DYNAMIC!>>=<!> d
    d <!DEBUG_INFO_DYNAMIC!>><!> d

    for (i in <!DEBUG_INFO_DYNAMIC!>d<!>) {
        i.<!DEBUG_INFO_DYNAMIC!>foo<!>()
    }

    val (<!DEBUG_INFO_DYNAMIC!>a<!>, <!DEBUG_INFO_DYNAMIC!>b<!>, <!DEBUG_INFO_DYNAMIC!>c<!>) = d
    a.<!DEBUG_INFO_DYNAMIC!>foo<!>()
    b.<!DEBUG_INFO_DYNAMIC!>foo<!>()
    c.<!DEBUG_INFO_DYNAMIC!>foo<!>()

    var dVar = d
    dVar<!DEBUG_INFO_DYNAMIC!>++<!>
    <!DEBUG_INFO_DYNAMIC!>++<!>dVar

    dVar<!DEBUG_INFO_DYNAMIC!>--<!>
    <!DEBUG_INFO_DYNAMIC!>--<!>dVar

    dVar <!DEBUG_INFO_DYNAMIC!>+=<!> 1
    dVar <!DEBUG_INFO_DYNAMIC!>-=<!> 1
    dVar <!DEBUG_INFO_DYNAMIC!>*=<!> 1
    dVar <!DEBUG_INFO_DYNAMIC!>/=<!> 1
    dVar <!DEBUG_INFO_DYNAMIC!>%=<!> 1

    d <!DEBUG_INFO_DYNAMIC!>+=<!> 1
    d <!DEBUG_INFO_DYNAMIC!>-=<!> 1
    d <!DEBUG_INFO_DYNAMIC!>*=<!> 1
    d <!DEBUG_INFO_DYNAMIC!>/=<!> 1
    d <!DEBUG_INFO_DYNAMIC!>%=<!> 1

    <!DEBUG_INFO_DYNAMIC!>d[1]<!> <!DEBUG_INFO_DYNAMIC!>+=<!> 1
    <!DEBUG_INFO_DYNAMIC!>d[1]<!> <!DEBUG_INFO_DYNAMIC!>-=<!> 1
    <!DEBUG_INFO_DYNAMIC!>d[1]<!> <!DEBUG_INFO_DYNAMIC!>*=<!> 1
    <!DEBUG_INFO_DYNAMIC!>d[1]<!> <!DEBUG_INFO_DYNAMIC!>/=<!> 1
    <!DEBUG_INFO_DYNAMIC!>d[1]<!> <!DEBUG_INFO_DYNAMIC!>%=<!> 1
}

val dyn: dynamic = null
val foo : Int <!DEBUG_INFO_DYNAMIC!>by dyn<!>
var bar : Int <!DEBUG_INFO_DYNAMIC!>by dyn<!>
