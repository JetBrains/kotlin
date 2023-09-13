// !DIAGNOSTICS: -NON_TOPLEVEL_CLASS_DECLARATION
// !MARK_DYNAMIC_CALLS

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

    d <!DEBUG_INFO_DYNAMIC!>and<!> d

    <!DEBUG_INFO_DYNAMIC!>d[1]<!>

    <!DEBUG_INFO_DYNAMIC!>d[1]<!> = 2

    <!DEBUG_INFO_DYNAMIC, DEBUG_INFO_DYNAMIC!>d[1]<!><!DEBUG_INFO_DYNAMIC!>++<!>
    <!DEBUG_INFO_DYNAMIC!>++<!><!DEBUG_INFO_DYNAMIC, DEBUG_INFO_DYNAMIC!>d[1]<!>

    <!DEBUG_INFO_DYNAMIC, DEBUG_INFO_DYNAMIC!>d[1]<!><!DEBUG_INFO_DYNAMIC!>--<!>
    <!DEBUG_INFO_DYNAMIC!>--<!><!DEBUG_INFO_DYNAMIC, DEBUG_INFO_DYNAMIC!>d[1]<!>

    <!DEBUG_INFO_DYNAMIC!>d()<!>
    <!DEBUG_INFO_DYNAMIC!>d(1)<!>
    <!DEBUG_INFO_DYNAMIC!>d(name = 1)<!>
    <!DEBUG_INFO_DYNAMIC!>d {}<!>

    class C {
        val plus: dynamic = null
    }

    C() + 5 // todo should be marked as DEBUG_INFO_DYNAMIC
    C().<!DEBUG_INFO_DYNAMIC!>plus(5)<!>

    d == d
    d != d

    d === d
    d !== d

    d <!DEBUG_INFO_DYNAMIC!><<!> d
    d <!DEBUG_INFO_DYNAMIC!><=<!> d
    d <!DEBUG_INFO_DYNAMIC!>>=<!> d
    d <!DEBUG_INFO_DYNAMIC!>><!> d

    for (i in d) {
        i.<!DEBUG_INFO_DYNAMIC!>foo<!>()
    }

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

