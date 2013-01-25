fun box() : String {
    val a = "lala"
    if(!a.identityEquals(a)) return "fail 1"
    if(a identityEquals a) return "OK"
    return "fail 2"
}
