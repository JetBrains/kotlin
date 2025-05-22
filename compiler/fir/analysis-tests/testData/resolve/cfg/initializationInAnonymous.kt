// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-77156

interface I {
    var i: Int?
    var j: Int
}

fun create() = object : I {
    init {
        i = 1
    }
    override var i: Int? = null

    init {
        j = 1
    }
    override var j: Int = 2
}
