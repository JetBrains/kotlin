fun box(): String {
    class Local {
        var result = "Fail"
    }

    val l = Local()
    (Local::result).set(l, "OK")
    return (Local::result).get(l)
}
