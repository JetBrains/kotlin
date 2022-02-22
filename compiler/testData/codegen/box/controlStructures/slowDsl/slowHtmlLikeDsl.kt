// TARGET_BACKEND: JVM
//  ^ this test might be rather slow

class Builder(var content: String)

fun Builder.begin(t: String) {
    content += "<$t>"
}

fun Builder.text(t: String) {
    content += t
}

fun Builder.end(t: String) {
    content += "</$t>"
}

fun err(e: Throwable) {}

inline fun Builder.tag(t: String, body: Builder.() -> Unit) {
    begin(t)
    try {
        body()
    } catch (e: Throwable) {
        err(e)
    } finally {
        end(t)
    }
}

inline fun Builder.t2(body: Builder.() -> Unit) {
    tag("t", body)
    tag("t", body)
}

val expectedLength = 1906

fun doStuff(b: Builder) {
    b.t2 { t2 { t2 { t2 { t2 { t2 { t2 { text("1") } } } } } } }
}

fun box(): String {
    val b = Builder("")
    doStuff(b)
    if (b.content.length != expectedLength)
        return "${b.content.length}"
    else
        return "OK"
}
