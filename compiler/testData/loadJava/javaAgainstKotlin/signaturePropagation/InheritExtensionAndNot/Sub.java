package test;

import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public interface Sub extends Super1, Super2 {
    @ExpectLoadError("Incompatible super methods: some are extension functions, some are not")
    void foo(String p);

    @ExpectLoadError("Incompatible super methods: some are extension functions, some are not|Incompatible super methods: some have vararg parameter, some have not")
    void bar(String... p);
}
