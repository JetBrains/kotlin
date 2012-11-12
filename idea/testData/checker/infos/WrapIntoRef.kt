fun refs() {
    var <info><warning>a</warning></info> = 1
    val <warning>v</warning> = {
      <info>a</info> = 2
    }

    var <info><warning>x</warning></info> = 1
    val <warning>b</warning> = object {
        fun foo() {
            <info>x</info> = 2
        }
    }

    var <info><warning>y</warning></info> = 1
    fun foo() {
        <info>y</info> = 1
    }
}

fun refsPlusAssign() {
    var <info>a</info> = 1
    val <warning>v</warning> = {
      <info>a</info> += 2
    }

    var <info>x</info> = 1
    val <warning>b</warning> = object {
        fun foo() {
            <info>x</info> += 2
        }
    }

    var <info>y</info> = 1
    fun foo() {
        <info>y</info> += 1
    }
}
