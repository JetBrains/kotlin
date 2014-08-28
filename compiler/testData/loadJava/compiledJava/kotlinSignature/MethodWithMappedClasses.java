package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class MethodWithMappedClasses {
    @KotlinSignature("fun <T> copy(dest : MutableList<in T>, src : List<T>)")
    public <T> void copy(List<? super T> dest, List<T> src) {
        throw new UnsupportedOperationException();
    }
}
