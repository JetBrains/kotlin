package test;

import java.lang.Number;
import java.util.*;

import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.jet.jvm.compiler.annotation.ExpectLoadError;

public class WrongProjectionKind {
    //@ExpectLoadError("Projection kind mismatch, actual: out, in alternative signature: ")
    @KotlinSignature("fun copy(a : Array<out Number>, b : Array<Number>) : MutableList<Number>")
    public List<Number> copy(Number[] from, Number[] to) {
        throw new UnsupportedOperationException();
    }
}
