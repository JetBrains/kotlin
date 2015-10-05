// !CHECK_TYPE

//KT-2505 Type mismatch: inferred type is T but T was expected

package a

interface MyType {}
class MyClass<T> : MyType {}

public open class HttpResponse() {
    public open fun <T> parseAs(dataClass : MyClass<T>) : T {
        throw Exception()
    }
    public open fun parseAs(dataType : MyType) : Any? {
        return null
    }
}

fun <R> test (httpResponse: HttpResponse, rtype: MyClass<R>) {
    val res = httpResponse.parseAs( rtype )
    checkSubtype<R>(res) //type mismatch: required R, found T
}
