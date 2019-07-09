// IGNORE_BACKEND: JVM_IR
fun test(ss: List<String?>) {
    val shortStrings = hashSetOf<String>()
    val longStrings = hashSetOf<String>()
    for (s in ss) {
        s?.let {
            if (s.length < 4) {
                shortStrings.add(s)
            }
            else {
                longStrings.add(s)
            }
        }
    }
}

// 2 POP
// 0 INVOKESTATIC java/lang/Boolean\.valueOf
// 0 CHECKCAST java/lang/Boolean
// 0 ACONST_NULL
