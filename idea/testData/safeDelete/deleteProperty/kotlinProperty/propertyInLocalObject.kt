open class SampleParent { var field = 0 }

fun context() {
    var v = object : SampleParent() { var ad<caret>dition = 0 }
    println(v.field)
    println(v.addition)
}
