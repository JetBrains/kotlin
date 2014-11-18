import kotlin.jvm.*
import kotlin.platform.*

class WithNative {
    class object {
        platformStatic native fun bar() {}
    }
}

object ObjWithNative {
    platformStatic native fun bar() {}
}

fun box(): String {
    try {
        WithNative.bar()
        return "Link error expected"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {}

    try {
        ObjWithNative.bar()
        return "Link error expected on object"
    }
    catch (e: java.lang.UnsatisfiedLinkError) {}
    return "OK"
}