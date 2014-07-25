package client;

import server.*

class Client {
    fun fooBar() {
        A.OBJECT$.setFoo("a");
        A.foo = "a";
        println("a.foo = " + A.OBJECT$.getFoo());
        println("a.foo = " + A.foo);
    }
}