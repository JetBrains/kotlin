// FIR_IDENTICAL

fun refs() {
    var <warning>a</warning> = 1
    val <warning>v</warning> = {
      <info>a</info> = 2
    }

    var <warning>x</warning> = 1
    val <warning>b</warning> = object {
        fun foo() {
            <info>x</info> = 2
        }
    }

    var <warning>y</warning> = 1
    fun foo() {
        <info>y</info> = 1
    }
}

fun refsPlusAssign() {
    var a = 1
    val <warning>v</warning> = {
      <info>a</info> += 2
    }

    var x = 1
    val <warning>b</warning> = object {
        fun foo() {
            <info>x</info> += 2
        }
    }

    var y = 1
    fun foo() {
        <info>y</info> += 1
    }
}
