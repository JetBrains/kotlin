// IGNORE_BACKEND: JVM_IR
open class A {
    inline fun test(a: Int = 1, b: Long = 1L, c: String = "123") {
        val d = 1
    }
}

//
// 1 test\$default\(LA;IJLjava/lang/String;ILjava/lang/Object;\)V\s+L0
// 1 LOCALVARIABLE this LA; L0 L9 0
// 1 LOCALVARIABLE a I L0 L9 1
// 1 LOCALVARIABLE b J L0 L9 2
// 1 LOCALVARIABLE c Ljava/lang/String; L0 L9 4
// 1 LOCALVARIABLE \$i\$f\$test I L6 L9 5
// 1 LOCALVARIABLE d I L8 L9 6


