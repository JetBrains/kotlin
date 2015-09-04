// PARAM_TYPES: kotlin.String
// PARAM_DESCRIPTOR: public final fun kotlin.String.foo(): kotlin.Unit defined in X

fun print(a: Any) {

}

class X {
    fun String.foo() {
        <selection>print(extension)</selection>
    }

    val String.extension: Int get() = length()
}