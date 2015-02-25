package client;

import server.*;

class Client {
    void fooBar() {
        A.Default.setFoo("a");
        A.foo = "a";
        System.out.println("a.foo = " + A.Default.getFoo());
        System.out.println("a.foo = " + A.foo);
    }
}