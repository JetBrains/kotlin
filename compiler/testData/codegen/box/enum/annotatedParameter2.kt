// TARGET_BACKEND: JVM
// FILE: Foo.java

public class Foo {

    static String test() {
        return KEnum.O.name() + KEnum.O.getValue();
    }
}



// FILE: KEnum.kt
@Retention(AnnotationRetention.RUNTIME)
annotation class A

enum class KEnum(@A val value: Any) {
    O("K") {
        fun foo() {}
    }
}

fun box(): String {
    return Foo.test()
}