// TARGET_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: ParameterNameTypeAttribute in ConeType
// MODULE: lib
// FILE: MFunction.java

public interface MFunction<T, R> {
    R invoke(T t);
}

// MODULE: main(lib)
// FILE: 1.kt


object Foo {
    class Requester(val dealToBeOffered: String)
}

class Bar {
    val foo = MFunction(Foo::Requester)
}

fun box(): String {
    return Bar().foo("OK").dealToBeOffered
}
