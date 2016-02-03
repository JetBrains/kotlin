package test

object ContentTypeByExtension {
    inline fun processRecords(crossinline operation: (String) -> String) =
            listOf("O", "K").map {
                val ext = B(it)
                operation(ext.toLowerCase())
            }.joinToString("")
}




inline fun A.toLowerCase(): String = (this as B).value

open class A

open class B(val value: String) : A()