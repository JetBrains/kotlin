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
    val x: Int
    fun f(): Int
    class C {
        constructor(x: String)
        val x: String
    }

    @JsName("default")
    object Default {
        val defaultX: Int
        fun defaultF(): Int
        class defaultC {
            constructor(x: String)

            val x: String
        }
    }
}


// FILE: main.kt
fun box(): String {
    if (named.x != 10) return "Fail1"
    if (named.f() != 10) return "Fail2"
    if (named.C("10").x != "10") return "Fail3"

    if (default.jsModule.Default.defaultX != 10) return "Fail4"
    if (default.jsModule.Default.defaultF() != 10) return "Fail5"
    if (default.jsModule.Default.defaultC("10").x != "10") return "Fail6"

    if (default.jsModule.x != 10) return "Fail7"
    if (default.jsModule.f() != 10) return "Fail8"
    if (default.jsModule.C("10").x != "10") return "Fail9"

    return "OK"
}