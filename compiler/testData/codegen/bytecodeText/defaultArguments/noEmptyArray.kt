// There is one ANEWARRAY instruction here, to generate the default parameter value.
fun default(vararg s: String = arrayOf("OK")) = s[0]

// This call on the other hand shouldn't allocate anything.
fun callDefault() = default()

// 1 ANEWARRAY