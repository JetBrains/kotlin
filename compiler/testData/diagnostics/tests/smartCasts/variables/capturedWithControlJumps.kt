// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-50092
// SKIP_TXT

fun test1() {
    var x: String? = "..."
    var lambda: (() -> Int)? = null
    for (i in 1..2) {
        when (i) {
            2 -> x = null
            1 -> if (x != null) lambda = { <!DEBUG_INFO_SMARTCAST!>x<!>.length } // bad
        }
    }
    lambda?.invoke()
}

fun test2() {
    var x: String? = "..."
    if (x != null) {
        val lambda = { <!SMARTCAST_IMPOSSIBLE!>x<!>.length } // bad
        while (false) return
        x = null
        lambda()
    }
}

fun test3() {
    var x: String? = "asd"
    if (x != null) {
        var lambda: (() -> Int)? = null
        try {
            lambda = { <!SMARTCAST_IMPOSSIBLE!>x<!>.length } // bad
            if (true) throw RuntimeException("...")
            return
        } catch (e: Exception) {
            x = null
            lambda?.invoke()
        } finally {
            x = null
            lambda?.invoke()
        }
    }
}

fun test4() {
    var x: String? = "..."
    if (x != null) {
        var lambda: (() -> Int)? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>
        while (true) {
            lambda = { <!SMARTCAST_IMPOSSIBLE!>x<!>.length } // bad
            if (true) break
            return
        }
        x = null
        lambda?.invoke()
    }
}

fun test5() {
    var lambda: (() -> Int)? = null
    for (i in 1..2) {
        lambda = {
            var x: String?
            x = ""
            <!DEBUG_INFO_SMARTCAST!>x<!>.length // ok
        }
    }
    lambda?.invoke()
}

fun test6() {
    var lambda: () -> Unit = { }
    for (x in 1..10) {
        var s: String? = null
        for (y in 1..10) {
            s = null
            lambda()
            s = ""
            lambda = { <!DEBUG_INFO_SMARTCAST!>s<!>.length } // bad - next iteration will assign s = null
        }

        if (s != null) {
            lambda = { <!DEBUG_INFO_SMARTCAST!>s<!>.length } // ok - s about to go out of scope
        }
    }
}
