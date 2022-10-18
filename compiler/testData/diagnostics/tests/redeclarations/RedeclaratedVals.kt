class Foo {
    fun normal() : Unit {
        val <!REDECLARATION!>someVal<!> = "One"
        val <!NAME_SHADOWING, REDECLARATION!>someVal<!> = "Two"
    }

    fun redeclarationInBlock() : Unit {
        val <!REDECLARATION!>a<!> = "A"
        {
            val <!NAME_SHADOWING!>a<!> = "A"
        }
        val <!NAME_SHADOWING, REDECLARATION!>a<!> = "B"
    }

    fun redeclarationInBlock1() : Unit {
        val <!REDECLARATION!>a<!> = "A"
        {
            val <!NAME_SHADOWING, REDECLARATION!>a<!> = "A"
            val <!NAME_SHADOWING, REDECLARATION!>a<!> = "B"
        }
        val <!NAME_SHADOWING, REDECLARATION!>a<!> = "B"
    }

    fun redeclarationInFunc() : Unit {
        class Inner {
            fun InnerFun(): Unit {
                val <!REDECLARATION!>a<!> = "A"
                val <!NAME_SHADOWING, REDECLARATION!>a<!> = "B"
            }
        }

        Inner()
    }

    fun redeclarationInStatements() : Unit {
        val a = 100
        if (true) {
            val <!NAME_SHADOWING, REDECLARATION!>a<!> = "A"
            val <!NAME_SHADOWING, REDECLARATION!>a<!> = "B"
            {
                val <!NAME_SHADOWING!>a<!> = "C"
            }
        }

        when (true) {
            true -> {
                val <!NAME_SHADOWING, REDECLARATION!>a<!> = "A"
                val <!NAME_SHADOWING, REDECLARATION!>a<!> = "B"
                {
                    val <!NAME_SHADOWING!>a<!> = "C"
                }
            }
            else -> {}
        }

        for (i in 1..10) {
            val <!NAME_SHADOWING, REDECLARATION!>a<!> = "A"
            val <!NAME_SHADOWING, REDECLARATION!>a<!> = "B"
            {
                val <!NAME_SHADOWING!>a<!> = "C"
            }
        }

        while (true) {
            val <!NAME_SHADOWING, REDECLARATION!>a<!> = "A"
            val <!NAME_SHADOWING, REDECLARATION!>a<!> = "B"
            {
                val <!NAME_SHADOWING!>a<!> = "C"
            }
        }

        <!UNREACHABLE_CODE!>do {
            val <!NAME_SHADOWING, REDECLARATION!>a<!> = "A"
            val <!NAME_SHADOWING, REDECLARATION!>a<!> = "B"
            {
                val <!NAME_SHADOWING!>a<!> = "C"
            }
            for (i in 1..10) {
                val <!NAME_SHADOWING, REDECLARATION!>a<!> = "A"
                val <!NAME_SHADOWING, REDECLARATION!>a<!> = "B"
                {
                    val <!NAME_SHADOWING!>a<!> = "C"
                }
            }
        } while (true)<!>
    }
}
