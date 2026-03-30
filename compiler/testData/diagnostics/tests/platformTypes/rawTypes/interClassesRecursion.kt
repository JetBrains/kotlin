// RUN_PIPELINE_TILL: BACKEND
// FILE: JavaScriptParser.java
public class JavaScriptParser<F extends JSPsiTypeParser> {}
// FILE: JSPsiTypeParser.java
public class JSPsiTypeParser<T extends JavaScriptParser> {}
