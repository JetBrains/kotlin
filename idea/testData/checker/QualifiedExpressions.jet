package qualified_expressions

fun test(s: String?) {
   val <warning>a</warning>: Int = <error>s?.length</error>
   val b: Int? = s?.length
   val <warning>c</warning>: Int = s?.length ?: -11
   val <warning>d</warning>: Int = s?.length ?: <error>"empty"</error>
   val e: String = <error>s?.length</error> ?: "empty"
   val <warning>f</warning>: Int = s?.length ?: b ?: 1
   val <warning>g</warning>: Boolean? = e.startsWith("s")//?.length
}

fun String.startsWith(<warning>s</warning>: String): Boolean = true