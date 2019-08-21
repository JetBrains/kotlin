// "Create expected property in common module testModule_Common" "true"
// SHOULD_FAIL_WITH: You cannot create the expect declaration from:,actual val &lt;T: A&gt; Some&lt;T&gt;.foo: Some&lt;T&gt; get() = TODO()
// DISABLE-ERRORS
interface A

actual val <T: A> Some<T>.<caret>foo: Some<T> get() = TODO()