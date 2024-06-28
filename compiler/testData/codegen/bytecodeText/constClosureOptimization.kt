// LAMBDAS: CLASS

fun test() {

    fun local(){
        val lam = {
            //static instance access
            local()
        }
        lam()
    }

    //static instance access
    val lam = {
        //static instance access
        local()
    }
    lam()

    //static instance access
    val cr = ::local
    cr()
}

// JVM_TEMPLATES
// 3 GETSTATIC ConstClosureOptimizationKt\$test\$1\.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$lam\$1\.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$cr\$1\.INSTANCE

// JVM_IR_TEMPLATES
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$cr\$1.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$lam\$1.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$local\$lam\$1.INSTANCE
