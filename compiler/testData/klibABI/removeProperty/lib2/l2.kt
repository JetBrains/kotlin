fun qux(exp: Boolean): String = if (exp) exp_foo else foo
fun qux2(exp: Boolean): String = if (exp) A().exp_foo else A().foo
fun qux3(exp: Boolean): String = if (exp) B().exp_foo else B().foo
