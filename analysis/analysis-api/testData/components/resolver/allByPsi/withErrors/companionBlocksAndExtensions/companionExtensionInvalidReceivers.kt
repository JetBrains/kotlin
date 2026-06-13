// LANGUAGE: +CompanionBlocksAndExtensions
package test

class C
class G<T>
object O

companion fun C?.nullableReceiver() {}
companion fun O.objectReceiver() {}
companion inline fun <reified T> T.typeParameterReceiver() {}
companion fun G<String>.typeArgumentsReceiver() {}

fun usage() {
    C.nullableReceiver()
    O.objectReceiver()
    G.typeArgumentsReceiver()
}
