import kotlin.contracts.*

fun returnValue(): Any? = null
fun always(): Boolean
infix fun Boolean.implies(condition: Boolean)

class EncryptedData
class Data {
    fun foo() {}
}
class Key

fun decrypt(data: EncryptedData, key: Key) = Data()

fun EncryptedData?.decrypt(key: Key?): Data? {
    contract {
        (this@decrypt != null && key != null) implies (returnValue() != null)
    }
    return if (this != null && key != null) decrypt(this, key) else null
}

fun test(encrypted: EncryptedData, key: Key) {
    val decrypted = encrypted.decrypt(key)
    decrypted.foo() // OK
}

fun test(encrypted: EncryptedData?, key: Key) {
    val decrypted = encrypted.decrypt(key)
    decrypted.<!INAPPLICABLE_CANDIDATE!>foo<!>() // NOT OK
}

fun test(encrypted: EncryptedData, key: Key?) {
    val decrypted = encrypted.decrypt(key)
    decrypted.<!INAPPLICABLE_CANDIDATE!>foo<!>() // NOT OK
}

fun test(encrypted: EncryptedData?, key: Key?) {
    val decrypted = encrypted.decrypt(key)
    decrypted.<!INAPPLICABLE_CANDIDATE!>foo<!>() // NOT OK
}