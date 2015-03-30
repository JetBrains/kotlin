package second

fun String?.helloFun() {
}

fun String.helloWithParams(i : Int) : String {
    return ""
}

fun String.helloFunPreventAutoInsert() {
}

fun <T: CharSequence> T.helloFunGeneric() {
}

fun Int.helloFake() {
}