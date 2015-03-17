fun box(): String {
    val o = "O"
    val ok_L = {o + "K"}
    class OK {
        val ok = ok_L()
    }
    return OK().ok
}