package client;

import server.TraitWithDelegatedWithImpl;

public class Test {
    public static void bar(TraitWithDelegatedWithImpl some) {
        some.foo();
    }
}
