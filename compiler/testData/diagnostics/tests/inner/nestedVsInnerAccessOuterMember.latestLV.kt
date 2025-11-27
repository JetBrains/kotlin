// RUN_PIPELINE_TILL: FRONTEND
// NI_EXPECTED_FILE
// LATEST_LV_DIFFERENCE

class Outer {
    fun function() = 42
    val property = ""
    
    class Nested {
        fun f() = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>function<!>()
        fun g() = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>property<!>
        fun h() = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>this@Outer<!>.function()
        fun i() = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>this@Outer<!>.property
    }
    
    inner class Inner {
        fun innerFun() = function()
        val innerProp = property
        fun innerThisFun() = this@Outer.function()
        val innerThisProp = this@Outer.property
        
        inner class InnerInner {
            fun f() = innerFun()
            fun g() = innerProp
            fun h() = this@Inner.innerFun()
            fun i() = this@Inner.innerProp
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inner, integerLiteral, nestedClass, propertyDeclaration,
stringLiteral, thisExpression */
