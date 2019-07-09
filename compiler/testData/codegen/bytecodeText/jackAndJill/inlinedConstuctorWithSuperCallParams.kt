// IGNORE_BACKEND: JVM_IR
open class A(val z: String) {

}

inline fun test(crossinline s: () -> String) {
    object : A("123") {
        val x = s();
    }
}

fun main(args: Array<String>) {
    var z = "123";
    test { z }
}

/*Threre are two constuctors so we should be sure that we check LOCALVARIABLEs from same method*/
// 1 LOCALVARIABLE this LInlinedConstuctorWithSuperCallParamsKt\$main\$\$inlined\$test\$1; L0 L8 0\s+LOCALVARIABLE \$super_call_param\$1 Ljava/lang/String; L0 L8 1
