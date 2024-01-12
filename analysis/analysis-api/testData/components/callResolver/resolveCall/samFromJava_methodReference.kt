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
    public void act(MyInterface actor) {
        if (actor != null) {
            actor.act();
        }
    }
}

// FILE: main.kt

package test.pkg

fun callback() {}

fun test(handler: MyHandler, list: List<MyInterface>) {
    <expr>handler.act(::callback)</expr>
}
