annotation class Anno(val s: String)

@Deprecated("function") @Anno("function")
fun f<caret>ooo(@Deprecated("a") @Anno("a") a: Int) {

}