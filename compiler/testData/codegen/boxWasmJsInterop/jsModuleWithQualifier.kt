// ES_MODULES
// FILE: jsModuleWithQualifier.mjs
let a = {
    b: {
        c: {
            d: {
                x: 10,
                f() { return 10; },
                C: class C {
                    constructor(x) {
                        this.x = x;
                    }
                }
            }
        }
    }
};

export { a };

// FILE: lib1.kt
@file:JsQualifier("a.b.c.d")
@file:JsModule("./jsModuleWithQualifier.mjs")

package abcd

external var x: Int
external fun f(): Int
external class C {
    constructor(x: String)
    val x: String
}

@JsName("x")
external var x2: Int

@JsName("f")
external fun f2(): Int

@JsName("C")
external class C2 {
    constructor(x: String)
    @JsName("x")
    val x2: String
}

// FILE: lib2.kt
@file:JsQualifier("a")
@file:JsModule("./jsModuleWithQualifier.mjs")
package a

external object b {
    class c {
        companion object {
            @JsName("d")
            object d2 {
                var x: Int
                fun f(): Int
                class C {
                    constructor(x: String)
                    val x: String
                }

                @JsName("x")
                var x2: Int

                @JsName("f")
                fun f2(): Int

                @JsName("C")
                class C2 {
                    constructor(x: String)
                    @JsName("x")
                    val x2: String
                }
            }
        }
    }
}



// FILE: main.kt
fun box(): String {
    if (abcd.x != 10) return "Fail1"
    if (abcd.f() != 10) return "Fail2"
    if (abcd.C("10").x != "10") return "Fail3"

    if (abcd.x2 != 10) return "Fail4"
    if (abcd.f2() != 10) return "Fail5"
    if (abcd.C2("10").x2 != "10") return "Fail6"

    if (a.b.c.Companion.d2.x != 10) return "Fail7"
    if (a.b.c.Companion.d2.f() != 10) return "Fail8"
    if (a.b.c.Companion.d2.C("10").x != "10") return "Fail9"

    if (a.b.c.Companion.d2.x2 != 10) return "Fail10"
    if (a.b.c.Companion.d2.f2() != 10) return "Fail11"
    if (a.b.c.Companion.d2.C2("10").x2 != "10") return "Fail12"

    return "OK"
}