// KT-128 Support passing only the last closure if all the other parameters have default values

fun div(<!UNUSED_PARAMETER!>c<!> : String = "", <!UNUSED_PARAMETER!>f<!> : () -> Unit) {}
fun f() {
    div { // Nothing passed, but could have been...
      // ...
    }

    div (c = "foo") { // More things could have been passed
      // ...
    }
}
