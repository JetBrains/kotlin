// LAMBDAS: CLASS

operator fun (() -> String).getValue(thisRef: Any?, property: Any?) = this()

fun foo() {
    val prop by { "OK" }
}

// METHOD : ObjectInLocalPropertyDelegateKt$foo$prop$2$kotlin_Function0$0.invoke()Ljava/lang/String;
// VARIABLE : NAME=this TYPE=LObjectInLocalPropertyDelegateKt$foo$prop$2$kotlin_Function0$0; INDEX=0
