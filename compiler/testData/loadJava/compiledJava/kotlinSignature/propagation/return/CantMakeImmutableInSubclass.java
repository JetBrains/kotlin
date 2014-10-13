package test;

import org.jetbrains.annotations.NotNull;
import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public interface CantMakeImmutableInSubclass {

    public interface Super {
        @KotlinSignature("fun foo(): MutableCollection<String>")
        Collection<String> foo();

        void dummy(); // to avoid loading as SAM interface
    }

    public interface Sub extends Super {
        //@ExpectLoadError("Return type is changed to not subtype for method which overrides another: List<String>, was: MutableList<String>")
        @KotlinSignature("fun foo(): List<String>")
        List<String> foo();
    }
}
