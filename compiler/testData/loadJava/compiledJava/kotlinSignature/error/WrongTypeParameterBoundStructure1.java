package test;

import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class WrongTypeParameterBoundStructure1 {
    //@ExpectLoadError("'java.lang.Runnable?' type in method signature has 0 type arguments, while 'Runnable<Int>' in alternative signature has 1 of them")
    @KotlinSignature("fun <A, B : Runnable<Int>> foo(a : A, b : List<B>) where B : List<Cloneable>")
    public <A, B extends Runnable & List<Cloneable>> void foo(A a, List<? extends B> b) {
    }
}
