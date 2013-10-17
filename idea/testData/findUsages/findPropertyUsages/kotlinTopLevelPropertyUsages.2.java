package client;

import server.ServerPackage;

class JClient {
    void fooBar() {
        System.out.println("foo = " + ServerPackage.getFoo());
        System.out.println("length: " + ServerPackage.getFoo().length());
        ServerPackage.setFoo("");
    }
}