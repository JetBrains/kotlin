package client;

import server.*

class Client {
    fun fooBar() {
        A<String> a = new A<String>("");
        a.setFoo("a");
        println("a.foo = " + a.getFoo());

        B b = new B();
        b.setFoo("b");
        println("b.foo = " + b.getFoo());
    }
}