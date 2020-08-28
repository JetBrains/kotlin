// FIR_COMPARISON
class C {
    fun String.extFun() { }

    companion object {
        fun String.foo() {
            <caret>
        }
    }
}

fun String.nonMemberExtFun() {}

// EXIST: nonMemberExtFun
// ABSENT: extFun
