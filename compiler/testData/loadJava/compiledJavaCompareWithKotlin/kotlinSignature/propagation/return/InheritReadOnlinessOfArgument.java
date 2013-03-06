package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Collection;

import jet.runtime.typeinfo.KotlinSignature;

public interface InheritReadOnlinessOfArgument {

    public interface Super {
        @KotlinSignature("fun foo(): List<List<String>>>")
        List<List<String>> foo();
    }

    public interface Sub extends Super {
        List<List<String>> foo();
    }
}
