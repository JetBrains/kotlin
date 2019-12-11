// FILE: My.java

public interface My {
    String foo(String arg);
}

// FILE: test.kt

class Your {
    val x = My() {
        arg: String? ->
        var y = arg
        val z: String
        if (y != null) z = y
        else z = "42"
        z
    }
}