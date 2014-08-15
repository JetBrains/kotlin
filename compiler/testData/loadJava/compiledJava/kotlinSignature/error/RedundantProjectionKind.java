package test;

import java.lang.Number;
import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class RedundantProjectionKind {
    @KotlinSignature("fun foo(list: List<out Number>)")
    public void foo(List<Number> list) {
        throw new UnsupportedOperationException();
    }
}
