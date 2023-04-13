annotation class A(val x: String)

class Cell(var value: Int) {
    operator fun getValue(thisRef: Any?, kProp: Any?) = value

    operator fun setValue(thisRef: Any?, kProp: Any?, newValue: Int) {
        value = newValue
    }
}

@get:A("test1.get")
val test1 by Cell(1)

@get:A("test2.get")
@set:A("test2.set")
@setparam:A("test2.set.param")
var test2 by Cell(2)
