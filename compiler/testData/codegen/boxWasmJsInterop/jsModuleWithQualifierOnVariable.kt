// TARGET_BACKEND: WASM_JS
// FILE: jsModuleWithQualifierOnVariable.mjs
let a = {
    b: {
        x: 10,
        y: 20,
        // Must not be picked up instead of the declared names.
        default: 100,
    }
};

function getX() {
    return a.b.x;
}

export { a, getX };

// FILE: lib1.kt
package qualified

@Suppress("JS_MODULE_PROHIBITED_ON_VAR")
@JsModule("./jsModuleWithQualifierOnVariable.mjs")
@JsQualifier("a.b")
external var x: Int

@JsModule("./jsModuleWithQualifierOnVariable.mjs")
@JsQualifier("a.b")
external val y: Int

@JsModule("./jsModuleWithQualifierOnVariable.mjs")
@JsQualifier("a.b")
@JsName("y")
external val y1: Int

// FILE: lib2.kt
@file:JsModule("./jsModuleWithQualifierOnVariable.mjs")

package named

external fun getX(): Int

@JsName("getX")
external fun getX1(): Int

// FILE: main.kt
fun box(): String {
    if (qualified.x != 10) return "Fail1: ${qualified.x}"
    if (qualified.y != 20) return "Fail2: ${qualified.y}"
    if (qualified.y1 != 20) return "Fail3: ${qualified.y1}"

    // The variable is referenced through its qualifier, so the write lands on a plain JS object.
    qualified.x = 30
    if (qualified.x != 30) return "Fail4: ${qualified.x}"
    if (named.getX() != 30) return "Fail5: ${named.getX()}"
    if (qualified.y != 20) return "Fail6: ${qualified.y}"
    if (qualified.y1 != 20) return "Fail7: ${qualified.y1}"
    if (named.getX1() != 30) return "Fail8: ${named.getX1()}"

    return "OK"
}
