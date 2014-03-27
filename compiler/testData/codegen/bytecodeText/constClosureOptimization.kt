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

// 3 GETSTATIC _DefaultPackage\$test\$1\.instance\$
// 1 GETSTATIC _DefaultPackage\$test\$2\.instance\$
// 1 GETSTATIC _DefaultPackage\$test\$3\.instance\$