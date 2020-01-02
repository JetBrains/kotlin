fun f(s : String?) : Boolean {
    return foo@(s?.equals("a"))!!
}