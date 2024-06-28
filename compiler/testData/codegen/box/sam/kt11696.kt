// TARGET_BACKEND: JVM
// WITH_STDLIB
// SAM_CONVERSIONS: CLASS
//   ^ test checks reflection for synthetic classes
// MODULE: lib
// JVM_ABI_K1_K2_DIFF: KT-64954
// FILE: Promise.java
import org.jetbrains.annotations.NotNull;

interface Consumer<T> {
    void consume(T t);
}

public abstract class Promise<T> {
    @NotNull
    public abstract Promise<T> done(@NotNull Consumer<? super T> done);
}

// MODULE: main(lib)
// FILE: 1.kt
class User {
    fun use(promise: Promise<*>): Promise<*> {
        promise.done { }
        return promise
    }
}

fun box(): String {
    var result = ""
    User().use(
            object : Promise<CharSequence>() {
                override fun done(x: Consumer<in CharSequence?>): Promise<CharSequence> {
                    result = x.javaClass.genericInterfaces[0].toString()
                    return this
                }
            }
    )

    if (result != "interface Consumer") return "fail: $result"

    return "OK"
}
