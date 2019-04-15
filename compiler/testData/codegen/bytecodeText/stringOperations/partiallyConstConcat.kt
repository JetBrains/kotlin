// IGNORE_BACKEND: JVM
fun foo(a: String, b: String) {
    val s = a + "1" + "2" + 3 + 4L + b + 5.0 + 6F + '7'
    val c = "$a${"1"}2${3}${4L}$b${5.0}${6F}${'7'}"
}

// 2 NEW java/lang/StringBuilder
// 2 LDC "1234"
// 2 LDC "5.06.07"