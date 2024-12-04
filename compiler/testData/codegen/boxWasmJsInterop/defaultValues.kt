// FILE: defaultValues.js
function foo(x1 = "d1", x2 = "d2", x3 = "d3", x4 = "d4", x5 = "d5") {
    return `${x1} ${x2} ${x3} ${x4} ${x5}`;
}

const foo2 = foo;

class C {
    constructor(x1 = 100, x2 = 200) {
        this.x1 = x1;
        this.x2 = x2;
    }

    foo(x3 = 300, x4 = 400) {
        return `${this.x1} ${this.x2} ${x3} ${x4}`;
    }

    bar(x5 = new C(10, 20), x6 = new C(1, 2)) {
        return `${x5.foo()} ${x6.foo()}`;
    }
}

class Writable {
    foo(x = 10, y = "default") {
        return x + y;
    }
    end(cb = () => "default") {
        return cb()
    }
}


// FILE: defaultValues.kt

external fun foo(
    x1: String = definedExternally,
    x2: String = definedExternally,
    x3: String = definedExternally,
    x4: String = definedExternally,
    x5: String = definedExternally,
): String

external fun foo2(
    x1: String,
    x2: String,
    x3: String = definedExternally,
    x4: String,
    x5: String = definedExternally,
): String

external class C {
    constructor(x1: Int = definedExternally, x2: Int = definedExternally)
    val x1: Int
    val x2: Int
    fun foo(x3: Int = definedExternally, x4: Int = definedExternally): String
    fun bar(x5: C = definedExternally, x6: C = definedExternally) : String
}

open external class Writable: WritableStream {
    override fun foo(x: Int, y: String): String
    override fun end(cb: () -> String): String
}

external interface WritableStream {
    fun foo(
        x: Int = definedExternally,
        y: String = definedExternally
    ): String

    fun end(cb: () -> String = definedExternally): String
}


fun box(): String {
    if (foo() != "d1 d2 d3 d4 d5") return "Fail 1"
    if (foo(x1 = "x1", x3 = "x3", x5 = "x5") != "x1 d2 x3 d4 x5") return "Fail 2"
    if (foo("x1", "x2", "x3", "x4", "x5") != "x1 x2 x3 x4 x5") return "Fail 3"
    if (foo2("x1", "x2", x4 = "x4") != "x1 x2 d3 x4 d5") return "Fail 4"
    if (foo2("x1", "x2", "x3", "x4", "x5") != "x1 x2 x3 x4 x5") return "Fail 5"

    if (C(1, 2).foo(3, 4) != "1 2 3 4") return "Fail 6"
    if (C(1).foo(3) != "1 200 3 400") return "Fail 7"
    if (C(x2 = 2).foo(x4 = 4) != "100 2 300 4") return "Fail 8"
    if (C().foo() != "100 200 300 400") return "Fail 9"

    if (C().bar(C(), C()) != "100 200 300 400 100 200 300 400") return "Fail 10"
    if (C().bar() != "10 20 300 400 1 2 300 400") return "Fail 11"

    if (Writable().end({ "OK" }) != "OK") return "Fail 12"
    if (Writable().end() != "default") return "Fail 13"
    if (Writable().foo(x = 33) != "33default") return "Fail 14"
    if (Writable().foo(y = "OK") != "10OK") return "Fail 15"

    return "OK"
}