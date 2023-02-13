// !RENDER_DIAGNOSTICS_FULL_TEXT
// TARGET_BACKEND: JVM_IR

// KT-30616
val foo = "hello"

<!SCRIPT_CAPTURING_ENUM!>enum class Bar(val s: String) {
    Eleven(s = foo)
}<!>
