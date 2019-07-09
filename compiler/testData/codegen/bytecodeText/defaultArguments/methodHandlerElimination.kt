// IGNORE_BACKEND: JVM_IR
//open modality to method handle check generation
open class A {
    inline fun test(p: String = "OK"): String {
        return p
    }
}

fun box(): String {
    return A().test()
}

//handler check in test$default
// 1 IFNULL
//mask check in test$default
// 1 IFEQ
//total ifs
// 2 IF