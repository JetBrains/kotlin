// FILE: KotlinFile.kt
class KotlinClass {
    public fun doSomething(runnable: Runnable) { runnable.run() }
}

public interface I {
    public fun doIt()
}

fun foo(javaClass: JavaClass, kotlinClass: KotlinClass) {
    javaClass.doSomething {
        bar()
    }

    kotlinClass.doSomething {
        bar()
    }

    javaClass.doSomething2 <!ARGUMENT_TYPE_MISMATCH!>{
        bar()
    }<!>
}

fun bar(){}

// FILE: JavaClass.java
public class JavaClass {
    public void doSomething(Runnable runnable) { runnable.run(); }
    public void doSomething2(I i) { i.doIt(); }
}
