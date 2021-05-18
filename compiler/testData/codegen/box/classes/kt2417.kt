// KJS_WITH_FULL_RUNTIME
fun box() : String{
    val set = HashSet<String>()
    set.add("foo")
    val t1 = "foo" in set  // returns true, valid
    if(!t1) return "fail1"
    val t2 = "foo" !in set // returns true, invalid
    if(t2) return "fail2"
    val t3 = "bar" in set  // returns false, valid
    if(t3) return "fail3"
    val t4 = "bar" !in set // return false, invalid
    if(!t4) return "fail4"
    val t5 = when("foo") {
      in set -> true
      else -> false
    }
    if(!t5) return "fail5"
    val t6 = when("foo") {
      !in set -> true
      else -> false
    }
    if(t6) return "fail6"
    return "OK"
}
