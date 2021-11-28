// FIR_IDENTICAL
<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun run<!>(b: () -> Unit) {}

<!NO_EXPLICIT_VISIBILITY_IN_API_MODE!>fun test<!>() {
    run {

    }

    fun localFun() {}
    localFun()
    run(::localFun)

    val localFun2 = fun() {}
    run(localFun2)

    val lambda = {}
    run(lambda)
}
