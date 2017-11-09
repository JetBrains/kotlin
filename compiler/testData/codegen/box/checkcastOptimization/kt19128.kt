class A

class B

class C

fun foo(parameters: Any?): Any? {
    var payload: Any? = null

    if (parameters != null) {
        if (parameters is A || parameters is B) {
            payload = parameters
        } else {
            payload = "O"
        }
    }

    if (payload is String) {
        payload += "K"
    }

    return payload
}

fun box(): String =
        "${foo(C())}"