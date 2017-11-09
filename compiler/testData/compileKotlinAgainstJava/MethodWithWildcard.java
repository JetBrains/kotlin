package test;

import java.util.Collection;

public class MethodWithWildcard<V> {

    public Collection<V> method(Collection<? extends V> c) { return null; }

}
