package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import jet.runtime.typeinfo.KotlinSignature;

public interface AddNullabilitySameGenericType1 {

    public interface Super {
        @KotlinSignature("fun foo(): MutableList<String>")
        List<String> foo();
    }

    public interface Sub extends Super {
        @KotlinSignature("fun foo(): MutableList<String?>")
        List<String> foo();
    }
}