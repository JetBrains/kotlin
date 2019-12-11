package a

import java.lang.Deprecated as deprecated
import java.lang.SuppressWarnings as suppresswarnings


@deprecated @suppresswarnings val s: String = "";

@deprecated @suppresswarnings fun main() {
    System.out.println("Hello, world!")
}

class Test(@deprecated val s: String,
           @suppresswarnings val x : Int) {}

