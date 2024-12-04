context(s: String)
class C {
    context(s: String)
    constructor() {}
}

context(s: String)
fun f(): String = s + this@s

context(_: String)
val p: String get() = f()

context(s: String)
var p2: String
    get() = s + this@s
    set(value) {
        s + this@s
    }

context(`_`: Any)
fun escapedBackTick() {}