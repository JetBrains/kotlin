package test;

import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class WrongTypeVariance {
    @ExpectLoadError("Variance mismatch, actual: out, in alternative signature: ")
    @KotlinSignature("fun copy(a : List<out Number>, b : List<Number>) : MutableList<Number>")
    public List<Number> copy(List<? extends Number> from, List<? extends Number> to) {
        throw new UnsupportedOperationException();
    }
}
