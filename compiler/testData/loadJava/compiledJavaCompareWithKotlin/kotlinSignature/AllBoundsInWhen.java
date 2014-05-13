package test;

import jet.runtime.typeinfo.KotlinSignature;

import java.io.Serializable;

public class AllBoundsInWhen {
    @KotlinSignature("fun <T> foo() where T: Serializable")
    public <T extends Serializable> void foo() {
        throw new UnsupportedOperationException();
    }
}
