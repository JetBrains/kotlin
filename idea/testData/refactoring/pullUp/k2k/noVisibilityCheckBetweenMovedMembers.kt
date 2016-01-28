open class Temp1
open class <caret>Temp2 : Temp1() {
    // INFO: {"checked": "true"}
    private val used: Int = 1
    // INFO: {"checked": "true"}
    private val using: Int = used + 1
}
class Temp3 : Temp2()