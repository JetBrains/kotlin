// FILE: 1.kt
class A(val s: String)

inline fun inlineMe(limit: Int, c: (String) -> String): A {
    var index = 0
    var res: A?
    while (true) {
        res = A(c(try {
            throw IllegalStateException("")
        } catch (ignored: Throwable) {
            "OK"
        })
        )
        if (index++ == limit) break
    }
    return res!!
}

// FILE: 2.kt
fun box(): String {
    return inlineMe(1) { "OK" }.s
}
