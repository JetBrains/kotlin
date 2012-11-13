package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Collection;

import jet.runtime.typeinfo.KotlinSignature;

public interface InheritReadOnlinessSameClass {

    public interface Super {
        @KotlinSignature("fun foo(): List<String>")
        List<String> foo();
    }

    public interface Sub extends Super {
        List<String> foo();
    }
}
