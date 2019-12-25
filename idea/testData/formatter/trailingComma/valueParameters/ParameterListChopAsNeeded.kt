class A1 {
    val x: String
    val y: String

    constructor(
        x: String,
        y: String,
    ) {
        this.x = x
        this.y = y
    }
}

class B1 {
    val x: String
    val y: String

    constructor(
        x: String,
        y: String
    ) {
        this.x = x
        this.y = y
    }
}

class C1 {
    val x: String
    val y: String

    constructor(
        x: String,
        y: String,) {
        this.x = x
        this.y = y
    }
}

class D1 {
    val x: String
    val y: String

    constructor(
        x: String,
        y: String
        ,) {
        this.x = x
        this.y = y
    }
}

class A2 {
    val x: String
    val y: String
    val z: String

    constructor(
        x: String,
        y: String,
        z: String,
    ) {
        this.x = x
        this.y = y
        this.z = z
    }
}

class B2 {
    val x: String
    val y: String
    val z: String

    constructor(
        x: String,
        y: String,
        z: String
    ) {
        this.x = x
        this.y = y
        this.z = z
    }
}

class C2 {
    val x: String
    val y: String
    val z: String

    constructor(
        x: String,
        y: String,
        z: String,) {
        this.x = x
        this.y = y
        this.z = z
    }
}

class D2 {
    val x: String
    val y: String
    val z: String

    constructor(
        x: String,
        y: String,
        z: String
        ,) {
        this.x = x
        this.y = y
        this.z = z
    }
}

class A3 {
    val x: String

    constructor(x: String,) {
        this.x = x
    }
}

class B3 {
    val x: String

    constructor(x: String) {
        this.x = x
    }
}

class C3 {
    val x: String

    constructor(
        x: String,) {
        this.x = x
    }
}

class D3 {
    val x: String

    constructor(
        x: String
        ,) {
        this.x = x
    }
}

class E1 {
    val x: String
    val y: String
    val z: String

    constructor(
        x: String,
        y: String, z: String,) {
        this.x = x
        this.y = y
        this.z = z
    }
}

class E2 {
    val x: String
    val y: String
    val z: String

    constructor(
        x: String,
        y: String, z: String) {
        this.x = x
        this.y = y
        this.z = z
    }
}
class A1(
    val x: String,
    y: String,
)

class B1(
    val x: String,
    val y: String
)

class C1(
    val x: String,
    val y: String,)

class D1(
    val x: String,
    val y: String
    ,)

class A2(
    val x: String,
    val y: String,
    val z: String,
)

class B2(
    val x: String,
    val y: String,
    val z: String
)

class C2(
    val x: String,
    val y: String,
    val z: String,)

class D2(
    val x: String,
    val y: String,
    val z: String
    ,)

class A3(
    val x: String,
)

class B3(
    val x: String
)

class C3(
    val x: String,)

class D3(
    val x: String
    ,)

class A4(
    val x: String   ,
    val y: String,
    val z: String   ,
)

class B4(
    val x: String,
    val y: String,
    val z: String
)

class C4(
    val x: String,
    val y: String,
    val z: String   ,)

class D4(
    val x: String,
    val y: String,
    val z: String
    ,   )

class E1(
    val x: String, val y: String,
    val z: String
    ,   )

class E2(
    val x: String, val y: String, val z: String
)

class C(
    z: String, val v: Int, val x: Int =
        42, val y: Int =
        42
)

val foo1: (Int, Int) -> Int = fun(
    x,
    y,
): Int = 42

val foo2: (Int, Int) -> Int = fun(
    x,
    y
): Int {
    return x + y
}

val foo3: (Int, Int) -> Int = fun(
    x, y,
): Int {
    return x + y
}

val foo4: (Int) -> Int = fun(
    x,
): Int = 42

val foo5: (Int) -> Int = fun(
    x
): Int = 42

val foo6: (Int) -> Int = fun(x,): Int = 42

val foo7: (Int) -> Int = fun(x): Int = 42

val foo8: (Int, Int, Int) -> Int = fun (x, y: Int, z,): Int {
    return x + y
}

val foo9: (Int, Int, Int) -> Int = fun (
    x,
    y: Int,
    z,
): Int = 42

val foo10: (Int, Int, Int) -> Int = fun (
    x,
    y: Int,
    z: Int
): Int = 43

val foo10 = fun (
    x: Int,
    y: Int,
    z: Int
): Int = 43

val foo11 = fun (
    x: Int,
    y: Int,
    z: Int,
): Int = 43

val foo12 = fun (
    x: Int, y: Int, z: Int,
): Int = 43

val foo13 = fun (x: Int, y: Int, z: Int,
): Int = 43

val foo14 = fun (x: Int, y: Int, z: Int
                 ,): Int = 43

fun a1(
    x: String,
    y: String,
) = Unit

fun b1(
    x: String,
    y: String
) = Unit

fun c1(
    x: String,
    y: String,) = Unit

fun d1(
    x: String,
    y: String
    ,) = Unit

fun a2(
    x: String,
    y: String,
    z: String,
) = Unit

fun b2(
    x: String,
    y: String,
    z: String
) = Unit

fun c2(
    x: String,
    y: String,
    z: String,) = Unit

fun d2(
    x: String,
    y: String,
    z: String
    ,) = Unit

fun a3(
    x: String,
) = Unit

fun b3(
    x: String
) = Unit

fun c3(
    x: String,) = Unit

fun d3(
    x: String
    ,) = Unit

fun a4(
    x: String
    ,
    y: String,
    z: String   ,
) = Unit

fun b4(
    x: String,
    y: String,
    z: String
) = Unit

fun c4(x: String,
       y: String,
       z: String   ,) = Unit

fun d4(
    x: String,
    y: String,
    z: String
    ,   ) = Unit

fun foo(
    x: Int =
        42
) {
}

class C(
    val x: Int =
        42
)

class G(
    val x: String, val y: String
    = "", /* */ val z: String
)

class G(
    val x: String, val y: String
    = "" /* */, /* */ val z: String
)

class H(
    val x: String, /*
    */ val y: String,
    val z: String   ,)

class J(
    val x: String, val y: String , val z: String /*
     */
    ,   )

class K(
    val x: String, val y: String,
    val z: String
    ,   )

class L(
    val x: String, val y: String, val z: String
)

// SET_TRUE: ALLOW_TRAILING_COMMA
// SET_INT: METHOD_PARAMETERS_WRAP = 4
