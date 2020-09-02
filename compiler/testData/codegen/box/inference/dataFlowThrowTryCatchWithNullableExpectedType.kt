fun test0() {
    var str: String? = null
    str = try {
        "something"
    } catch (e: Exception) {
        "another"
    }

    str.length
}

fun test1(b: Boolean): String {
    var enumVar: SomeEnum? = null
    val result = if (b) {
        "OK"
    } else {
        enumVar = try {
            SomeEnum.valueOf("OK")
        } catch (e: Exception) {
            throw RuntimeException()
        }
        enumVar.name
    }

    return result
}

enum class SomeEnum {
    OK, BAR
}

fun box(): String {
    test0()
    return test1(false)
}