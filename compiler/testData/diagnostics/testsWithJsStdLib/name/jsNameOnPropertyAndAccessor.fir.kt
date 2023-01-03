package foo

class A {
    @JsName("x_") val x: Int
        @JsName("get_x") get() = 23

    @JsName("y_") val y = 0

    @JsName("m_") var m: Int
        @JsName("get_m") get() = 23
        @JsName("set_m") set(value) {}
}

@JsName("xx_") val xx: Int
    @JsName("get_xx") get() = 23

@JsName("yy_") val yy = 0

@JsName("mm_") var mm: Int
    @JsName("get_mm") get() = 23
    @JsName("set_mm") set(value) {}
