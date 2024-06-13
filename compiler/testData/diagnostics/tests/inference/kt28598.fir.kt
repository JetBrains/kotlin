// DIAGNOSTICS: -UNUSED_EXPRESSION

fun case_1(a: MutableList<out MutableList<MutableList<MutableList<MutableList<MutableList<MutableList<Int?>?>?>?>?>?>?>?) {
    if (a != null) {
        val b = a[0] // no SMARTCAST diagnostic
        if (b != null) {
            val c = b[0]
            if (c != null) {
                val d = c[0]
                if (d != null) {
                    val e = d[0]
                    if (e != null) {
                        val f = e[0]
                        if (f != null) {
                            val g = f[0]
                            if (g != null) {
                                val h = g<!NO_GET_METHOD!>[0]<!>
                                if (h != null) {
                                    h.<!UNRESOLVED_REFERENCE!>inc<!>()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


fun case_2(a: MutableList<out MutableList<MutableList<MutableList<out MutableList<MutableList<MutableList<out Int?>?>?>?>?>?>?>?) {
    if (a != null) {
        val b = a[0] // no SMARTCAST diagnostic
        if (b != null) {
            val c = b[0]
            if (c != null) {
                val d = c[0]
                if (d != null) {
                    val e = d[0] // no SMARTCAST diagnostic
                    if (e != null) {
                        val f = e[0]
                        if (f != null) {
                            val g = f[0]
                            if (g != null) {
                                val h = g<!NO_GET_METHOD!>[0]<!> // no SMARTCAST diagnostic
                                if (h != null) {
                                    h.<!UNRESOLVED_REFERENCE!>inc<!>()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


fun case_3(a: MutableList<MutableList<MutableList<MutableList<MutableList<MutableList<MutableList<Int?>?>?>?>?>?>?>?) {
    if (a != null) {
        val b = a[0]
        if (b != null) {
            val c = b[0]
            if (c != null) {
                val d = c[0]
                if (d != null) {
                    val e = d[0]
                    if (e != null) {
                        val f = e[0]
                        if (f != null) {
                            val g = f[0]
                            if (g != null) {
                                val h = g[0]
                                if (h != null) {
                                    h.inc()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
