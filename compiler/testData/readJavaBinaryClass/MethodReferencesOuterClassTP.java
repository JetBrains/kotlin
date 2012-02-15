package test;

final class Outer<P> {
    final class Inner {
        final <Q extends P> void f() {}
    }
}
