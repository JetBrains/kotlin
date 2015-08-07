fun box(): String {
    class Local {
        fun result(s: String) = s
    }

    return Local::result.call(Local(), "OK")
}
