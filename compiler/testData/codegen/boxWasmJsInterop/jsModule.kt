// ES_MODULES
// FILE: jsModule.mjs
let x = 10;
function f() { return 10; };
class C {
    constructor(x) {
        this.x = x;
    }
}

export { x, f, C };
export default { defaultX: x, defaultF: f, defaultC: C };

// FILE: lib1.kt
@file:JsModule("./jsModule.mjs")

package named

external val x: Int
external fun f(): Int
external class C {
    constructor(x: String)
    val x: String
}

// FILE: lib2.kt
package default

@JsModule("./jsModule.mjs")
external object jsModule {
    val defaultX: Int
    fun defaultF(): Int
    class defaultC {
        constructor(x: String)

        val x: String
    }
}


// FILE: main.kt
fun box(): String {
    if (named.x != 10) return "Fail1"
    if (named.f() != 10) return "Fail2"
    if (named.C("10").x != "10") return "Fail3"

    if (default.jsModule.defaultX != 10) return "Fail4"
    if (default.jsModule.defaultF() != 10) return "Fail5"
    if (default.jsModule.defaultC("10").x != "10") return "Fail6"

    return "OK"
}