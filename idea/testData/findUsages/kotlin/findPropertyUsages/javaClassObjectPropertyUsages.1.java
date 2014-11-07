package client;

import server.*;

class Client {
    void fooBar() {
        A.OBJECT$.setFoo("a");
        A.foo = "a";
        System.out.println("a.foo = " + A.OBJECT$.getFoo());
        System.out.println("a.foo = " + A.foo);
    }
}