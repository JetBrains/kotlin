// TARGET_BACKEND: JVM
// WITH_COROUTINES
// WITH_RUNTIME
// MODULE: lib
// FILE: lib.kt
interface I {}

suspend inline fun foo() = object : I {}

// MODULE: useLib(lib)
// FILE: UseLib.java
import kotlin.coroutines.*;
import kotlin.Unit;

public class UseLib {
    public static String useFoo() {
        Object i = LibKt.foo(new MyContinuation());
        return i.getClass().getName() + " " + i.getClass().getEnclosingClass().getName() + " " + i.getClass().getEnclosingClass().getEnclosingClass();
    }
}

class MyContinuation implements Continuation<I> {
    public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
    }
    public void resumeWith(Object value) {}
}

// MODULE: main(useLib)
// FILE: main.kt

fun box(): String {
    val res = UseLib.useFoo()
    if (res == "LibKt\$foo\$2 LibKt null") {
        return "OK"
    } else {
        return res
    }
}
