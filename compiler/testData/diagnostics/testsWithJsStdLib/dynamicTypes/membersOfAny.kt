// !MARK_DYNAMIC_CALLS

fun test(d: dynamic) {
    d == 1

    d.equals(1)
    d?.equals(1)
    run {
        d!!.equals(1)
    }

    d.hashCode()
    d?.hashCode()
    run {
        d!!.hashCode()
    }

    d.toString()
    d?.toString()
    run {
        d!!.toString()
    }
}