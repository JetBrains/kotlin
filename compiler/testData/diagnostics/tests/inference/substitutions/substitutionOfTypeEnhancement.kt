// FIR_IDENTICAL
// FULL_JDK
// WITH_STDLIB
// WITH_REFLECT

// FILE: NonNullApi.java

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.PACKAGE})
@javax.annotation.Nonnull
@javax.annotation.meta.TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface NonNullApi { }

// FILE: Task2.java

public class Task2 implements Task {
    void foo() {}
}

// FILE: Test.java

public class Test {
    <T extends Task> void register(Class<T> var2, Foo<? super T> var3) throws IllegalAccessException, InstantiationException {
        var3.execute(var2.newInstance());
    }
}

// FILE: Foo.java

@NonNullApi
public interface Foo<T> {
    void execute(T t);
}

// FILE: Task.java

public interface Task {}

// FILE: main.kt

fun main() {
    Test().register(Task2::class.java) { // before the fix, type parameter's type leaked here (type of `it` is `T`)
        it.foo()
        it.apply {
            foo()
        }
    }
}
