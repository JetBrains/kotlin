fun foo(): Int  {
    return 42
}

fun box(): String  {
    val f: Int = foo()
    val tmp0_subject: Int = f
    when
 {
        ((tmp0_subject == 12)) ->  {
            return "FAIL"
        }
        else ->  {
            return "OK"
        }
    }
}
