// IGNORE_BACKEND: JVM_IR
// TODO KT-36637 Trivial closure optimizatin in JVM_IR

fun test() {

    fun local(){
        {
            //static instance access
            local()
        }()
    }

    //static instance access
    {
        //static instance access
        local()
    }()

    //static instance access
    (::local)()
}

// 3 GETSTATIC ConstClosureOptimizationKt\$test\$1\.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$2\.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$3\.INSTANCE
