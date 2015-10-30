//NO_CHECK_LAMBDA_INLINING
import test.*

class Boxer {
    val box: () -> Box by injectFnc()
}

fun box(): String {
    val box = Box()
    registerFnc { box }
    val prop = Boxer().box
    if (prop() != box) return "fail 1"

    return "OK"
}