package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;

public interface InheritNullabilitySameGenericType {

    public interface Super {
        @KotlinSignature("fun foo(): MutableList<String>")
        List<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        List<String> foo();
    }
}