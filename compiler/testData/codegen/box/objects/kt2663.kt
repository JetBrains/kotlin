fun box() : String {
    var a = 1

    object {
        {
            a = 2
        }
    }
    return if (a == 2) "OK" else "fail"
}
