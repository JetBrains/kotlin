class MyClass {
    val prop: Int = 42
}

typealias MyClassAlias = MyClass
typealias MyInt = Int
typealias BodyType = () -> Unit
typealias BiIntPredicate = (Int, Int) -> Boolean

fun box(): String {
    val myClass = MyClassAlias()
    val tmp: MyInt = 42
    when (myClass.prop) {
        tmp -> return "OK"
        else -> return "FAIL"
    }
}