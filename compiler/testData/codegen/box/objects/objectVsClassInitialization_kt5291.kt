// KJS_WITH_FULL_RUNTIME
public inline fun <T> T.with(f: T.() -> Unit): T {
    this.f()
    return this
}

public class Cls {
    val string = "Cls"
    val buffer = StringBuilder().with {
        append(string)
    }
}

public object Obj {
    val string = "Obj"
    val buffer = StringBuilder().with {
        append(string)
    }
}

fun box(): String {
    if (Cls().buffer.toString() != "Cls") return "Fail class"
    if (Obj.buffer.toString() != "Obj") return "Fail object"
    return "OK"
}
