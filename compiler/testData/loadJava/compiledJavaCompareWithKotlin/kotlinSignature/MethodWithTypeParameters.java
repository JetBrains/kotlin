package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import java.util.BitSet;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;

public class MethodWithTypeParameters {
    @KotlinSignature("fun <A, B : Runnable> foo(a : A, b : List<B>, c: MutableList<in String?>) where B : List<Cloneable>")
    public <A, B extends Runnable & List<Cloneable>> void foo(A a, List<? extends B> b, List<? super String> list) {
    }
}
