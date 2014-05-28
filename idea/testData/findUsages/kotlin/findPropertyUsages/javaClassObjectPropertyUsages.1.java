package client;

import server.*

class Client {
    fun fooBar() {
        A.object$.setFoo("a");
        A.foo = "a";
        println("a.foo = " + A.object$.getFoo());
        println("a.foo = " + A.foo);
    }
}