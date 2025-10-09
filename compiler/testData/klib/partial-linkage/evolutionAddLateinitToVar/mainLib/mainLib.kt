@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

val x = X()

fun lib(): String {

    val a = try {
        qux 
    } catch (e: UninitializedPropertyAccessException) {
        "uninitiaized"
    }

    val b = try {
        x.bar
    } catch(e: UninitializedPropertyAccessException) {
        "uninitiaized"
    }

    qux = "new global value"
    x.bar = "new member value"

    return when {
        a != "uninitiaized" -> "fail 1"
        b != "uninitiaized" -> "fail 2"
        qux != "new global value" -> "fail 3"
        x.bar != "new member value" -> "fail 4"

        else -> "OK"
    }
}

