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

    kotlinClass.doSomething <!TYPE_MISMATCH!>{
        bar()
    }<!>

    javaClass.doSomething2 <!TYPE_MISMATCH!>{
        bar()
    }<!>
}

fun bar(){}

// FILE: JavaClass.java
public class JavaClass {
    public void doSomething(Runnable runnable) { runnable.run(); }
    public void doSomething2(I i) { i.doIt(); }
}