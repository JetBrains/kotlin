package foo

class A

@JsName("xx") val A.x: Int
    get() = 23

@property:JsName("yy") val A.y: Int
    get() = 42
