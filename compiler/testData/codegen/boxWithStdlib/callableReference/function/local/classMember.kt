fun box(): String {
    class Local {
        fun foo() = "OK"
    }

    val ref = Local::foo
    return Local().ref()
}
