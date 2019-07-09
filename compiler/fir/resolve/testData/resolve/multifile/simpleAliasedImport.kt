package a
import b.MyClass as HisClass
import b.foo as foo2
import b.I as I2

class YourClass : HisClass()

fun bar() {
    foo2()
    I2()
}
