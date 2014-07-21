package test;

import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class WrongTypeParameterBoundStructure2 {
    @ExpectLoadError("'kotlin.List<kotlin.Cloneable>?' type in method signature has 1 type arguments, while 'List' in alternative signature has 0 of them")
    @KotlinSignature("fun <A, B : Runnable> foo(a : A, b : List<B>) where B : List")
    public <A, B extends Runnable & List<Cloneable>> void foo(A a, List<? extends B> b) {
    }
}
