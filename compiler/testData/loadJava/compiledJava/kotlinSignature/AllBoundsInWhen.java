package test;

import java.io.Serializable;

public class AllBoundsInWhen {
    public <T extends Serializable> void foo() {
        throw new UnsupportedOperationException();
    }
}
