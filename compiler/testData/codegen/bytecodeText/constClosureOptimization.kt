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

// JVM_TEMPLATES
// 3 GETSTATIC ConstClosureOptimizationKt\$test\$1\.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$2\.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$3\.INSTANCE

// JVM_IR_TEMPLATES
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$1.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$2.INSTANCE
// 1 GETSTATIC ConstClosureOptimizationKt\$test\$local\$1.INSTANCE