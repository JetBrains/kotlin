// !DIAGNOSTICS: -NON_TOPLEVEL_CLASS_DECLARATION
// !MARK_DYNAMIC_CALLS

fun test(d: dynamic) {
    +d
    -d
    ! d


    d + d
    d + 1
    "" + d

    d - d
    d * d
    d / d
    d % d

    d and d

    d[1]

    d[1] = 2

    d[1]++
    ++d[1]

    d[1]--
    --d[1]

    d()
    d(1)
    d(name = 1)
    d <!VARARG_OUTSIDE_PARENTHESES!>{}<!>

    class C {
        val plus: dynamic = null
    }

    C() <!PROPERTY_AS_OPERATOR!>+<!> 5 // todo should be marked as DEBUG_INFO_DYNAMIC
    C().plus(5)

    d == d
    d != d

    d === d
    d !== d

    d < d
    d <= d
    d >= d
    d > d

    for (i in d) {
        i.foo()
    }

    var dVar = d
    dVar++
    ++dVar

    dVar--
    --dVar

    dVar += 1
    dVar -= 1
    dVar *= 1
    dVar /= 1
    dVar %= 1

    d += 1
    d -= 1
    d *= 1
    d /= 1
    d %= 1

    d[1] += 1
    d[1] -= 1
    d[1] *= 1
    d[1] /= 1
    d[1] %= 1
}
