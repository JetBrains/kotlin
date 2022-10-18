// FIR_IDENTICAL
class Foo {
    fun normal() : Unit {
        val <!REDECLARATION!>someVal<!> = "One"
        val <!REDECLARATION!>someVal<!> = "Two"
    }

    fun redeclarationInBlock() : Unit {
        val <!REDECLARATION!>a<!> = "A"
        {
            val a = "A"
        }
        val <!REDECLARATION!>a<!> = "B"
    }

    fun redeclarationInBlock1() : Unit {
        val <!REDECLARATION!>a<!> = "A"
        {
            val <!REDECLARATION!>a<!> = "A"
            val <!REDECLARATION!>a<!> = "B"
        }
        val <!REDECLARATION!>a<!> = "B"
    }

    fun redeclarationInFunc() : Unit {
        class Inner {
            fun InnerFun(): Unit {
                val <!REDECLARATION!>a<!> = "A"
                val <!REDECLARATION!>a<!> = "B"
            }
        }

        Inner()
    }

    fun redeclarationInStatements() : Unit {
        val a = 100
        if (true) {
            val <!REDECLARATION!>a<!> = "A"
            val <!REDECLARATION!>a<!> = "B"
            {
                val a = "C"
            }
        }

        when (true) {
            true -> {
                val <!REDECLARATION!>a<!> = "A"
                val <!REDECLARATION!>a<!> = "B"
                {
                    val a = "C"
                }
            }
            else -> {}
        }

        for (i in 1..10) {
            val <!REDECLARATION!>a<!> = "A"
            val <!REDECLARATION!>a<!> = "B"
            {
                val a = "C"
            }
        }

        while (true) {
            val <!REDECLARATION!>a<!> = "A"
            val <!REDECLARATION!>a<!> = "B"
            {
                val a = "C"
            }
        }

        do {
            val <!REDECLARATION!>a<!> = "A"
            val <!REDECLARATION!>a<!> = "B"
            {
                val a = "C"
            }
            for (i in 1..10) {
                val <!REDECLARATION!>a<!> = "A"
                val <!REDECLARATION!>a<!> = "B"
                {
                    val a = "C"
                }
            }
        } while (true)
    }
}
