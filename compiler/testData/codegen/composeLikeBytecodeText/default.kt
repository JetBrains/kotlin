fun foo(s: String = "O") = s

fun box() = foo() + foo("K")

// For Compose special default arugment handling, we still do not want
// the default argument mask in the local variable table.

// 0 \$default