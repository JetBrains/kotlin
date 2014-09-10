package test;

import org.jetbrains.annotations.NotNull;
import java.util.List;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface AddNullabilitySameGenericType2 {

    public interface Super {
        @KotlinSignature("fun foo(): MutableList<String>")
        List<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        //@ExpectLoadError("Auto type 'kotlin.MutableList<kotlin.String>' is not-null, while type in alternative signature is nullable: 'MutableList<String>?'")
        @KotlinSignature("fun foo(): MutableList<String>?")
        List<String> foo();
    }
}
