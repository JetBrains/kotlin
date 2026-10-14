// PROPERTY_WRITE_COUNT: E$Companion$instance count=1
// PROPERTY_NOT_WRITTEN_TO: E$Companion$instance scope=E$static_init
// PROPERTY_NOT_WRITTEN_TO: E$Companion$instance scope=E$Companion$getInstance
// PROPERTY_NOT_WRITTEN_TO: E$Companion$instance scope=E$Companion IGNORED_BACKENDS=JS_IR_ES6
// PROPERTY_NOT_WRITTEN_TO: E$Companion$instance scope=new_E_Companion_fso24c_k$ TARGET_BACKENDS=JS_IR_ES6
enum class E {
    A, B;

    companion object {
        // No fields in this companion
        fun ok() = "OK"
    }
}

fun box(): String {
    return E.ok()
}
