public fun testCoalesce() {
   val value: String = when {
       true -> {
          if (true) {
             "foo"
          } else {
             "bar"
          }
       }
       else -> "Hello world"
    }
}

fun box(): String {
    testCoalesce()
    return "OK"
}