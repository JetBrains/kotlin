// FILE: externals.js
function createObject() {
    return {};
}

function setX(obj, x) {
    obj.x = x;
}

function getX(obj) {
    return obj.x;
}

// FILE: externals.kt
external interface Obj
external fun createObject(): Obj
external fun setX(obj: Obj, x: Int)
external fun getX(obj: Obj): Int

fun box(): String {
    val obj = createObject()
    setX(obj, 100)
    if (getX(obj) != 100) return "Fail 2"
    return "OK"
}