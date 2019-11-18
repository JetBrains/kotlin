// IGNORE_BACKEND_FIR: JVM_IR
val String.id: String
    get() = this

fun box(): String {
    val pr = String::id

    if (pr.get("123") != "123") return "Fail value: ${pr.get("123")}"

    if (pr.name != "id") return "Fail name: ${pr.name}"

    return pr.get("OK")
}
