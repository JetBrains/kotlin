// !LANGUAGE: +InlineClasses

inline class Foo(val x: Int) {
    fun empty() {}
    fun param(y: String) {}
    fun Any.extension() {}
    fun Any.extensionAndParam(y: Double) {}

    fun withInlineClassType(c: Foo) {}
}

// method: Foo$Erased::empty
// jvm signature: (I)V
// generic signature: null

// method: Foo$Erased::param
// jvm signature: (ILjava/lang/String;)V
// generic signature: null

// method: Foo$Erased::extension
// jvm signature: (ILjava/lang/Object;)V
// generic signature: null

// method: Foo$Erased::extensionAndParam
// jvm signature: (ILjava/lang/Object;D)V
// generic signature: null

// method: Foo$Erased::withInlineClassType-1e4ch6lh
// jvm signature: (II)V
// generic signature: null
