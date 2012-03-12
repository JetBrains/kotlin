// KT-201 Allow to call extension with nullable receiver with a '.'

fun <T : Any> T?.npe() : T = if (this == null) throw NullPointerException() else this

fun foo() {
  val i : Int? = 1
  i.npe() // error!
}
