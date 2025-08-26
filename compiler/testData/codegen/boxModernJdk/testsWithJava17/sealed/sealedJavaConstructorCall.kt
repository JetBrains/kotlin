// LANGUAGE: +AllowCallingJavaOpenSealedClassConstructor
// MODULE: lib
// FILE: SealedLib.java
public sealed class SealedLib permits SealedLib.Sub {
    public SealedLib() {}

    public static final class Sub extends SealedLib {
        public Sub() {}
    }
}

// MODULE: main(lib)
// FILE: Sealed.java
public sealed class Sealed permits Sealed.Sub {
    public Sealed() {}

    public static final class Sub extends Sealed {
        public Sub() {}
    }
}

// FILE: box.kt
fun box(): String {
    SealedLib()
    Sealed()
    return "OK"
}
