package foo

class A {
    var x: Int
        @JsName("get_x") get() = 23
        set(value) {}

    var y: Int
        get() = 23
        @JsName("set_y") set(value) {}

    var z: Int
        @JsName("get_z") get() = 23
        @JsName("set_z") set(value) {}
}

var xx: Int
    @JsName("get_xx") get() = 23
    set(value) {}

var A.ext: Int
    @JsName("get_ext") get() = 23
    set(value) {}
