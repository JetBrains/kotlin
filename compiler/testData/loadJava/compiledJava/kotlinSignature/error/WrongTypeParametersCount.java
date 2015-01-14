package test;

import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.kotlin.jvm.compiler.annotation.ExpectLoadError;

public class WrongTypeParametersCount {
    @ExpectLoadError("Method signature has 2 type parameters, but alternative signature has 3")
    @KotlinSignature("fun <A, B, C> foo(a : A, b : List<B>)")
    public <A, B> void foo(A a, List<? extends B> b) {
    }
}
