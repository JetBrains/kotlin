// FILE: MFunction.java

public interface MFunction<T, R> {
    R invoke(T t);
}

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