package b;

import a.Outer.Inner;

public class X extends Inner.A {
    Inner.A next = new Inner.A();
}