// WITH_STDLIB

// FILE: MyInterface.java

package test.pkg;

interface MyInterface {
    void act();
}

// FILE: MyHandler.java

package test.pkg;

import java.util.List;

public class MyHandler {
    public void stash(MyInterface actor, List<MyInterface> actors) {
        actors.add(actor);
    }
}

// FILE: main.kt

package test.pkg

fun test(handler: MyHandler, list: List<MyInterface>) {
    val lambda = { println("hello") }
    <expr>handler.stash(lambda, list)</expr>
}
