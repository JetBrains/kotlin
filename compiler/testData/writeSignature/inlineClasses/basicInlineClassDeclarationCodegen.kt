// !LANGUAGE: +InlineClasses

inline class Foo(val x: Int) {
    fun empty() {}
    fun param(y: String) {}
    fun Any.extension() {}
    fun Any.extensionAndParam(y: Double) {}

    fun withInlineClassType(c: Foo) {}
}

// method: Foo::empty-impl
// jvm signature: (I)V
// generic signature: null

// method: Foo::param-impl
// jvm signature: (ILjava/lang/String;)V
// generic signature: null

// method: Foo::extension-impl
// jvm signature: (ILjava/lang/Object;)V
// generic signature: null

// method: Foo::extensionAndParam-impl
// jvm signature: (ILjava/lang/Object;D)V
// generic signature: null

// method: Foo::withInlineClassType-GWb7d6U
// jvm signature: (II)V
// generic signature: null
