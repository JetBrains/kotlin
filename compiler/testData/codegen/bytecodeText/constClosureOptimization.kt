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

// 1 GETSTATIC ConstClosureOptimizationKt\$test\$cr\$1.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$lam\$1.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$local\$lam\$1.INSTANCE
