fun s() = "1" + "2" + 3 + 4L + 5.0 + 6F + '7'
fun c() = "${"1"}2${3}${4L}${5.0}${6F}${'7'}"

// 0 NEW java/lang/StringBuilder
// 2 LDC "12345.06.07"