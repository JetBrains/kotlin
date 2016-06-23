class A {
    @property:JsName("x_") <!JS_NAME_ON_ACCESSOR_AND_PROPERTY!>@get:JsName("getX_")<!> val x: Int = 0

    <!JS_NAME_IS_NOT_ON_ALL_ACCESSORS!>@get:JsName("getY_") var y: Int<!> = 0
}