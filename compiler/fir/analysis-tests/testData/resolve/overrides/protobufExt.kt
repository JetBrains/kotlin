interface Ext<M : Message<M>, T>
interface Message<M : Message<M>> {
    fun <T> ext(e: Ext<M, T>): T
}

class MyMessage : Message<MyMessage>
class MyExt : Ext<MyMessage, String>

fun <M : Message<M>, T> Message<M>.extF(e: Ext<M, T>): T = ext(e)

fun foo(m: MyMessage, e: MyExt) {
    m.ext(e)
    m.extF(e)
}
