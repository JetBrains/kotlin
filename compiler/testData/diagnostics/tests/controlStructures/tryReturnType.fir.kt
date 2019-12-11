// !WITH_NEW_INFERENCE
package a

fun foo() : Int {
    try {
        doSmth()
    }
    catch (e: Exception) {
        return ""
    }
    finally {
        return ""
    }
}

fun bar() : Int =
    try {
        doSmth()
    }
    catch (e: Exception) {
        ""
    }
    finally {
        ""
    }


fun doSmth() {}
