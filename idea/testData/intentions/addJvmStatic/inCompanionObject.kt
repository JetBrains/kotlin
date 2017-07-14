// WITH_RUNTIME
// INTENTION_TEXT: "Add '@JvmStatic' annotation"

class Test {
    companion object {
        fun <caret>main(args: Array<String>) {
        }
    }
}