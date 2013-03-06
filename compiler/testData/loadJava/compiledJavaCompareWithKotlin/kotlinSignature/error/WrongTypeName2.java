package test;

import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class WrongTypeName2 {
    @ExpectLoadError("Alternative signature type mismatch, expected: Something.String, actual: jet.String")
    @KotlinSignature("fun foo(a : Something.String) : String")
    public String foo(String a) {
        throw new UnsupportedOperationException();
    }
}
