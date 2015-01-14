package test;

import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class WrongTypeName3 {
    @ExpectLoadError("Alternative signature type mismatch, expected: List, actual: kotlin.String")
    @KotlinSignature("fun foo(a : String) : List")
    public String foo(String a) {
        throw new UnsupportedOperationException();
    }
}
