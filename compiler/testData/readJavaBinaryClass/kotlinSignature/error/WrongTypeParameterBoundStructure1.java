package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import java.util.BitSet;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;

public class WrongTypeParameterBoundStructure1 {
    @KotlinSignature("fun <A, B : Runnable<Int>> foo(a : A, b : List<out B>, c: List<in String?>) where B : List<Cloneable>")
    public <A, B extends Runnable & List<Cloneable>> void foo(A a, List<? extends B> b, List<? super String> list) {
    }
}
