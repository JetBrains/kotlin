fun lib(): String {
  val x = X()
  return when {
    foo_a("first") != "first" -> "fail 1"
    foo_b() != "foo_b default" -> "fail 2"
    foo_c("first", "second") != "first, second" -> "fail 3"
    foo_d("first", "second") != "firstsecond" -> "fail 4"
    foo_d(a = "first", b = "second") != "firstsecond" -> "fail 5"
    foo_e() != "foo_e default afoo_e default b" -> "fail 6"
    foo_e(a = "first", b = "second") != "firstsecond" -> "fail 7"
    foo_f("second", "third", b = "first") != "second, thirdfirst" -> "fail 8"

    x.bar_a("first") != "first" -> "fail 11"
    x.bar_b() != "foo_b default" -> "fail 12"
    x.bar_c("first", "second") != "first, second" -> "fail 13"
    x.bar_d("first", "second") != "firstsecond" -> "fail 14"
    x.bar_d(a = "first", b = "second") != "firstsecond" -> "fail 15"
    x.bar_e() != "foo_e default afoo_e default b" -> "fail 16"
    x.bar_e(a = "first", b = "second") != "firstsecond" -> "fail 17"
    x.bar_f("second", "third", b = "first") != "second, thirdfirst" -> "fail 18"

    qux_a("first").x != "first" -> "fail 21"
    qux_b().x != "foo_b default" -> "fail 22"
    qux_c("first", "second").x != "first, second" -> "fail 23"
    qux_d("first", "second").x != "firstsecond" -> "fail 24"
    qux_d(a = "first", b = "second").x != "firstsecond" -> "fail 25"
    qux_e().x != "foo_e default afoo_e default b" -> "fail 26"
    qux_e(a = "first", b = "second").x != "firstsecond" -> "fail 27"
    qux_f("second", "third", b = "first").x != "second, thirdfirst" -> "fail 28"

    else -> "OK"
  }
}

