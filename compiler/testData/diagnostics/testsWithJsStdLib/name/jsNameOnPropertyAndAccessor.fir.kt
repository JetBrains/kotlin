package foo

class A {
    @JsName("x_") val x: Int
        <!JS_NAME_ON_ACCESSOR_AND_PROPERTY!>@JsName("get_x")<!> get() = 23

    @JsName("y_") val y = 0

    @JsName("m_") var m: Int
        <!JS_NAME_ON_ACCESSOR_AND_PROPERTY!>@JsName("get_m")<!> get() = 23
        <!JS_NAME_ON_ACCESSOR_AND_PROPERTY!>@JsName("set_m")<!> set(value) {}
}

@JsName("xx_") val xx: Int
    <!JS_NAME_ON_ACCESSOR_AND_PROPERTY!>@JsName("get_xx")<!> get() = 23

@JsName("yy_") val yy = 0

@JsName("mm_") var mm: Int
    <!JS_NAME_ON_ACCESSOR_AND_PROPERTY!>@JsName("get_mm")<!> get() = 23
    <!JS_NAME_ON_ACCESSOR_AND_PROPERTY!>@JsName("set_mm")<!> set(value) {}
