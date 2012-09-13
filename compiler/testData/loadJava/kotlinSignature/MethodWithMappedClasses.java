package test;

import java.lang.UnsupportedOperationException;
import java.util.*;
import jet.runtime.typeinfo.KotlinSignature;

public class MethodWithMappedClasses {
    @KotlinSignature("fun <T> copy(dest : MutableList<in T>, src : List<out T>)")
    public <T> void copy(List<? super T> dest, List<? extends T> src) {
        throw new UnsupportedOperationException();
    }
}
