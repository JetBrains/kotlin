package test;

import java.lang.Number;
import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class ConflictingProjectionKind {
    @ExpectLoadError("Projection kind 'in' is conflicting with variance of jet.List")
    @KotlinSignature("fun foo(list: List<in Number>)")
    public void foo(List<Number> list) {
        throw new UnsupportedOperationException();
    }
}
