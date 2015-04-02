class C(val i: Int) {
    var status = "fail"

    [kotlin.jvm.overloads] constructor(o: String, k: String = "K"): this(-1) {
        status = o + k
    }
}

fun box(): String {
    val c = (javaClass<C>().getConstructor(javaClass<String>()).newInstance("O"))
    return c.status
}
