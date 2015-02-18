// KT-4351 Cannot resolve reference to self in init of class local to function

fun box(): String {
    var accessedFromConstructor: Class<*>? = null

    class MyClass() {
        init {
            accessedFromConstructor = javaClass<MyClass>()
        }
    }

    MyClass()
    if (accessedFromConstructor!!.getName().endsWith("MyClass")) {
        return "OK"
    } else {
        return accessedFromConstructor.toString()
    }
}
