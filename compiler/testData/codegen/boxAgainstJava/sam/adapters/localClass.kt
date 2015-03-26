var status: String = "fail"

fun box(): String {
    class C() : JavaClass({status = "OK"}) {}
    C().run()
    return status
}

