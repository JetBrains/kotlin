// WASM_FAILS_IN: SM
// FILE: externals.js
function createObject() {
    return {
        getXmethod() { return this.x; },
        setXmethod(v) { this.x = v; }
    };
}

function setX(obj, x) {
    obj.x = x;
}

function getX(obj) {
    return obj.x;
}

const readOnlyProp = 123;
var mutableProp = "20";

class C1 {
    constructor(a, b) {
        this.a = a;
        this.b = b;
    }

    getA() { return this.a; }
    setA(v) { this.a = v; }

    getB() { return this.b; }
}

C1.Nested1 = class {}
C1.Nested1.Nested2 = class {}
C1.Nested1.Nested2.Nested3 = class {
    constructor(x) {
        this.x = x;
    }

    foo() { return this.x + " from Nested 3"; }
}

class C2 extends C1 {
    constructor(a, b) {
        super(a, b);
        this.c = "C";
    }
}


C2.Object1 = { Object2: { Object3: { x: "C2.Object1.Object2.Object3.x" } } }

const externalObj = {
    x: "externalObj.x",
    y: {  x: "externalObj.y.x" },
    c: class { x = "(new externalObj.c()).x" }
}

function jsRenamed() {
    return 'renamed'
}

// FILE: externals.kt
external interface Obj {
    var x: Int
    fun getXmethod(): Int
    fun setXmethod(v: Int)
}
external fun createObject(): Obj
external fun setX(obj: Obj, x: Int)
external fun getX(obj: Obj): Int

external val readOnlyProp: Int
external var mutableProp: String

open external class C1 {
    constructor(a: String, b: String)
    var a: String
    val b: String

    fun getA(): String
    fun setA(x: String)
    fun getB(): String

    class Nested1 {
        class Nested2 {
            class Nested3 {
                constructor(x: String)
                fun foo(): String
            }
        }
    }
}

external class C2 : C1 {
    constructor(a: String, b: String)

    val c: String

    object Object1 {
        object Object2 {
            object Object3 {
                val x: String
            }
        }
    }
}

external object externalObj {
    val x: String
    object y {
        val x: String
    }
    class c {
        val x: String
    }
}

@JsName("jsRenamed")
external fun testJsName(): String

fun box(): String {
    val obj = createObject()
    setX(obj, 100)
    if (getX(obj) != 100) return "Fail 2"

    if (obj.x != 100) return "Fail 2.1"
    obj.x = 200
    if (getX(obj) != 200) return "Fail 2.2"
    val objXRef = obj::x
    objXRef.set(300)
    if (getX(obj) != 300 || obj.x != 300 || objXRef.get() != 300) return "Fail 2.3"

    if (obj.getXmethod() != 300) return "Fail 2.4"
    obj.setXmethod(400)
    if (obj.getXmethod() != 400 || getX(obj) != 400) return "Fail 2.5"

    if (readOnlyProp != 123) return "Fail 3"
    if (::readOnlyProp.get() != 123) return "Fail 4"
    if (mutableProp != "20") return "Fail 5"
    mutableProp = "30"
    if (mutableProp != "30") return "Fail 6"
    (::mutableProp).set("40")
    if (mutableProp != "40") return "Fail 7"

    val c1 = C1("A", "B")
    if (c1.a != "A" || c1.b != "B") return "Fail 8"
    if (c1.getA() != "A" || c1.getB() != "B") return "Fail 9"
    c1.setA("A2")
    if (c1.a != "A2") return "Fail 10"
    c1.a = "A3"
    if (c1.getA() != "A3") return "Fail 11"
    val c2 = C2("A", "B")
    if (c2.a != "A" || c2.b != "B" || c2.c != "C") return "Fail 12"
    val c2_as_c1: C1 = c2
    if (c2_as_c1.a != "A" || c2_as_c1.b != "B") return "Fail 13"

    val nested3 = C1.Nested1.Nested2.Nested3("example")
    if (nested3.foo() != "example from Nested 3") return "Fail 14"

    if (C2.Object1.Object2.Object3.x != "C2.Object1.Object2.Object3.x") return "Fail 15"
    if (externalObj.x != "externalObj.x") return "Fail 16"
    if (externalObj.y.x != "externalObj.y.x") return "Fail 17"
    if (externalObj.c().x != "(new externalObj.c()).x") return "Fail 18"


    if (c1 as Any !is C1) return "Fail 19"
    if (c2 as Any !is C1) return "Fail 20"
    if (c2 as Any !is C2) return "Fail 21"
    if (externalObj.c() as Any !is externalObj.c) return "Fail 22"
    if (10 as Any is C1) return "Fail 23"
    if (c1 as Any is C2) return "Fail 24"

    if (testJsName() != "renamed") return "Fail 25"

    return "OK"
}
