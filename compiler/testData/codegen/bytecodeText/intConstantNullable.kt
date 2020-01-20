val a : Int? = 10

fun foo() = a!!.toString()

// 1 checkNotNull \(Ljava/lang/Object;\)V
