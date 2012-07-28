package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import java.util.BitSet;
import java.util.List;

import jet.runtime.typeinfo.KotlinSignature;

public class WrongTypeParametersCount {
    @KotlinSignature("fun <A, B, C> foo(a : A, b : List<out B>)")
    public <A, B> void foo(A a, List<? extends B> b) {
    }
}
