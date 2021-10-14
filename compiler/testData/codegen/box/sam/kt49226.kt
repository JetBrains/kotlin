// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
//    java.lang.UnsupportedOperationException: This function has a reified type parameter and thus can only be inlined at compilation time,
//    not called directly.
//      at kotlin.jvm.internal.Intrinsics.throwUndefinedForReified(Intrinsics.java:207)
//      at kotlin.jvm.internal.Intrinsics.throwUndefinedForReified(Intrinsics.java:201)
//      at kotlin.jvm.internal.Intrinsics.reifiedOperationMarker(Intrinsics.java:211)
//      at Kt49226Kt$box$x$1.invoke(kt49226.kt:5)
//      at Kt49226Kt$box$x$1.invoke(kt49226.kt:5)
//      at Kt49226Kt$sam$Func$0.apply(kt49226.kt)
//      at Mapper.map(Mapper.java:20)
//      at Kt49226Kt.box(kt49226.kt:5)

// FILE: kt49226.kt
fun box(): String {
    val x: Array<String> = Mapper("OK").map(::arrayOf)
    return x[0]
}


// FILE: Func.java
public interface Func <T, R> { R apply(T t); }

// FILE: Mapper.java
public class Mapper<T> {
    T t;
    public Mapper(T t) {
        this.t = t;
    }
    public <R> R map(Func<T, R> f) {
        return f.apply(t);
    }
}
