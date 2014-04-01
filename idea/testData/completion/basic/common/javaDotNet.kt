class Java() {
    fun oracleRulezzz()
}

class DotNet {
    fun microsoftRulezzz()
}

fun Java.dot(): DotNet = DotNet()

fun test() {
    Java().<caret>
}

// EXIST: microsoftRulezzz