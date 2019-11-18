// IGNORE_BACKEND_FIR: JVM_IR
package mult_constructors_3_bug

public open class Identifier() {
    private var myNullable : Boolean = true
    companion object {
        open public fun init(isNullable : Boolean) : Identifier {
            val id = Identifier()
            id.myNullable = isNullable
            return id
        }
    }
}

fun box() : String {
    Identifier.init(true)
    return "OK"
}
