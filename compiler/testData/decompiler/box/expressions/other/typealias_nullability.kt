class MyClass {
    val prop: Int? = 42
}

typealias MyClassNullable = MyClass?
typealias MyIntNullable = Int?

fun box(): String {
    val myClass = MyClassNullable()
    val tmp: Int? = 42
    when (myClass.prop) {
        tmp -> return "OK"
        else -> return "FAIL"
    }
}