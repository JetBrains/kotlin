// FILE: JavaScriptParser.java
public class JavaScriptParser<T extends JSPsiTypeParser> {
    public String foo() {return "OK";}
}
// FILE: JSPsiTypeParser.java
public class JSPsiTypeParser<T extends JavaScriptParser> {}

// FILE: ES6Parser.java

public class ES6Parser<T extends JSPsiTypeParser> extends JavaScriptParser<T> {}

// FILE: main.kt

fun createParser(): JavaScriptParser<*> {
    return ES6Parser<JSPsiTypeParser<*>>()
}

fun box(): String = createParser().foo()

