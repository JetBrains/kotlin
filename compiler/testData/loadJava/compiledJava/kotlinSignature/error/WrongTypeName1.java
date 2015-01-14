package test;

import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class WrongTypeName1 {
    @ExpectLoadError("Alternative signature type mismatch, expected: Unit, actual: kotlin.String")
    @KotlinSignature("fun foo(a : String) : Unit")
    public String foo(String a) {
        throw new UnsupportedOperationException();
    }
}
