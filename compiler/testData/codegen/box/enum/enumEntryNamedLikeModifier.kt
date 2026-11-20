// ISSUE: KT-90707

enum class TransactionStatus(val code: String) {
    correct("correct"),
    error("error"),
    value("value") {},
    inline("inline"),
    ;
}

fun box(): String {
    if (TransactionStatus.error.code != "error") return "FAIL 1: ${TransactionStatus.error.code}"
    if (TransactionStatus.value.code != "value") return "FAIL 2: ${TransactionStatus.value.code}"
    if (TransactionStatus.inline.name != "inline") return "FAIL 3: ${TransactionStatus.inline.name}"
    if (TransactionStatus.values().size != 4) return "FAIL 4: ${TransactionStatus.values().size}"
    return "OK"
}
