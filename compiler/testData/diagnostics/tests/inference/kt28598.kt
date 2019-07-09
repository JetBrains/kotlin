// !WITH_NEW_INFERENCE
// !IDIAGNOSTICS: -UNUSED_EXPRESSION

fun case_1(a: MutableList<out MutableList<MutableList<MutableList<MutableList<MutableList<MutableList<Int?>?>?>?>?>?>?>?) {
    if (a != null) {
        val b = <!OI;DEBUG_INFO_SMARTCAST!>a<!>[0] // no SMARTCAST diagnostic
        if (b != null) {
            val c = <!DEBUG_INFO_SMARTCAST!>b<!>[0]
            if (c != null) {
                val d = <!DEBUG_INFO_SMARTCAST!>c<!>[0]
                if (d != null) {
                    val e = <!DEBUG_INFO_SMARTCAST!>d<!>[0]
                    if (e != null) {
                        val f = <!DEBUG_INFO_SMARTCAST!>e<!>[0]
                        if (f != null) {
                            val g = <!DEBUG_INFO_SMARTCAST!>f<!>[0]
                            if (g != null) {
                                val h = <!DEBUG_INFO_SMARTCAST!>g<!>[0]
                                if (h != null) {
                                    <!DEBUG_INFO_SMARTCAST!>h<!>.inc()
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
        val b = <!OI;DEBUG_INFO_SMARTCAST!>a<!>[0] // no SMARTCAST diagnostic
        if (b != null) {
            val c = <!DEBUG_INFO_SMARTCAST!>b<!>[0]
            if (c != null) {
                val d = <!DEBUG_INFO_SMARTCAST!>c<!>[0]
                if (d != null) {
                    val e = <!OI;DEBUG_INFO_SMARTCAST!>d<!>[0] // no SMARTCAST diagnostic
                    if (e != null) {
                        val f = <!DEBUG_INFO_SMARTCAST!>e<!>[0]
                        if (f != null) {
                            val g = <!DEBUG_INFO_SMARTCAST!>f<!>[0]
                            if (g != null) {
                                val h = <!OI;DEBUG_INFO_SMARTCAST!>g<!>[0] // no SMARTCAST diagnostic
                                if (h != null) {
                                    <!DEBUG_INFO_SMARTCAST!>h<!>.inc()
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
        val b = <!DEBUG_INFO_SMARTCAST!>a<!>[0]
        if (b != null) {
            val c = <!DEBUG_INFO_SMARTCAST!>b<!>[0]
            if (c != null) {
                val d = <!DEBUG_INFO_SMARTCAST!>c<!>[0]
                if (d != null) {
                    val e = <!DEBUG_INFO_SMARTCAST!>d<!>[0]
                    if (e != null) {
                        val f = <!DEBUG_INFO_SMARTCAST!>e<!>[0]
                        if (f != null) {
                            val g = <!DEBUG_INFO_SMARTCAST!>f<!>[0]
                            if (g != null) {
                                val h = <!DEBUG_INFO_SMARTCAST!>g<!>[0]
                                if (h != null) {
                                    <!DEBUG_INFO_SMARTCAST!>h<!>.inc()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}