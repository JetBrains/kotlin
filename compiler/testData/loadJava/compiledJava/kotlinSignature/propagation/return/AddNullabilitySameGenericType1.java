package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface AddNullabilitySameGenericType1 {

    public interface Super {
        @KotlinSignature("fun foo(): MutableList<String>")
        List<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        //@ExpectLoadError("Return type is changed to not subtype for method which overrides another: MutableList<String?>, was: MutableList<String>")
        @KotlinSignature("fun foo(): MutableList<String?>")
        List<String> foo();
    }
}
