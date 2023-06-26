// !RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// KT-30616

val foo = "hello"

<!SCRIPT_CAPTURING_ENUM!>enum class Bar(val s: String = foo) {

    Eleven("0")
}<!>
