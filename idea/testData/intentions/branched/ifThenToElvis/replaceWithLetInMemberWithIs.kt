// IS_APPLICABLE: false
// WITH_RUNTIME

class Test {
    fun doAThing(param1: String): String {
        return param1
    }

    fun doAThingIfPresent(param1: String?) {
        // In theory could propose transformation to 'let'
        <caret>if (param1 is String) {
            doAThing(param1)
        } else {
            ""
        }
    }
}