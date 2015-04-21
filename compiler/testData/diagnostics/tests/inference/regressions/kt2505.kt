// !CHECK_TYPE

//KT-2505 Type mismatch: inferred type is T but T was expected

package a

trait MyType {}
class MyClass<T> : MyType {}

public open class HttpResponse() {
    public open fun parseAs<T>(dataClass : MyClass<T>) : T {
        throw Exception()
    }
    public open fun parseAs(dataType : MyType) : Any? {
        return null
    }
}

fun test<R> (httpResponse: HttpResponse, rtype: MyClass<R>) {
    val res = httpResponse.parseAs( rtype )
    checkSubtype<R>(res) //type mismatch: required R, found T
}
