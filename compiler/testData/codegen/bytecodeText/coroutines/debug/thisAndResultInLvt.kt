suspend fun dummy() {}

val c: suspend () -> Unit = {
    dummy()
    dummy()
}

fun blackhole(a: Any) {}

class A {
    suspend fun foo(a: A, s: String = "", block: suspend A.() -> Unit) {
        block()
        block()
        blackhole(this)
        blackhole(a)
        blackhole(s)
        blackhole(block)
    }
}

// BEs generate continuation classes differently, JVM_IR generates more correctly

// foo, c's lambda and foo's continuation
// 3 LOCALVARIABLE \$result Ljava/lang/Object;

// foo and <init>
// 2 LOCALVARIABLE this LA;
// 1 LOCALVARIABLE a LA;
// 1 LOCALVARIABLE s Ljava/lang/String;
// 1 LOCALVARIABLE block Lkotlin/jvm/functions/Function2;
// 1 LOCALVARIABLE \$continuation Lkotlin/coroutines/Continuation;

// JVM_TEMPLATES
// invokeSuspend
// 1 LOCALVARIABLE this LThisAndResultInLvtKt\$c\$1;

// JVM_IR_TEMPLATES
// <init>, invoke, invoke bridge, invokeSuspend
// 4 LOCALVARIABLE this LThisAndResultInLvtKt\$c\$1; L0 L.* 0
