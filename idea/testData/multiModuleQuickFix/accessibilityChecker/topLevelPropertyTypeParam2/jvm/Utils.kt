// "Create expected property in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: You cannot create the expect declaration from:,actual val &lt;T&gt;Some&lt;T&gt;.foo: Some&lt;T&gt; get() = TODO()
// DISABLE-ERRORS

class Some<T>

actual val <T>Some<T>.<caret>foo: Some<T> get() = TODO()