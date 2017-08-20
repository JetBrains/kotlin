// !LANGUAGE: +CapturedInClosureSmartCasts

fun run(f: () -> Unit) = f()

fun foo(s: String?) {
    var x: String? = null
    if (s != null) {
        x = s
    }
    if (x != null) {
        run {
            <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
        }
    }
}

fun bar(s: String?) {
    var x = s
    if (x != null) {
        run {
            <!DEBUG_INFO_SMARTCAST!>x<!>.hashCode()
        }
    }
}

fun baz(s: String?) {
    var x = s
    if (x != null) {
        run {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
        }
        run {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
            x = null
        }
    }
}

fun gaz(s: String?) {
    var x = s
    if (x != null) {
        run {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
            x = null
        }
        run {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
        }
    }
}

fun gav(s: String?) {
    var x = s
    if (x != null) {
        run {
            <!SMARTCAST_IMPOSSIBLE!>x<!>.hashCode()
        }
        x = null
    }
}