// DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE, -NAME_SHADOWING, -DEBUG_INFO_SMARTCAST

fun invokeLater(block: () -> Unit) = Unit

fun testLoop(b: Boolean) {
    var x: Any = ""
    x as String
    invokeLater {
        x.length
    }
    x.length
    while (b) {
        x.length
        var x: Any = "hello"
        x = 1
    }
    x.length
    invokeLater {
        x.length
    }
}

fun testLocalFunction() {
    var x: Any = ""
    x as String
    invokeLater {
        x.length
    }
    x.length
    fun f() {
        x.length
        var x: Any = "hello"
        x = 1
    }
    x.length
    invokeLater {
        x.length
    }
}

fun testLocalClass() {
    var x: Any = ""
    x as String
    invokeLater {
        x.length
    }
    x.length
    class F {
        init {
            x.length
            var x: Any = "hello"
            x = 1
        }
    }
    x.length
    invokeLater {
        x.length
    }
}

fun testAnonymousClass() {
    var x: Any = ""
    x as String
    invokeLater {
        x.length
    }
    x.length
    val f = object {
        init {
            x.length
            var x: Any = "hello"
            x = 1
        }
    }
    x.length
    invokeLater {
        x.length
    }
}

fun testLambda() {
    var x: Any = ""
    x as String
    invokeLater {
        x.length
    }
    x.length
    invokeLater {
        x.length
        var x: Any = "hello"
        x = 1
    }
    x.length
    invokeLater {
        x.length
    }
}
