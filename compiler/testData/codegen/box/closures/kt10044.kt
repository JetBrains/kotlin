open class JClass() {
    fun test(): String {
        return "OK"
    }
}

class Example : JClass {
    constructor() : super()

    private var obj: JClass? = null

    var result: String? = null

    init {
        {
            result = obj?.test()
        }()
    }
}

class Example2 : JClass {
    constructor() : super()

    private var obj: JClass? = this

    var result: String? = null

    init {
        {
            result = obj?.test()
        }()
    }
}


fun box(): String {
    val result = Example().result
    if (result != null) "fail 1: $result"

    return Example2().result!!
}