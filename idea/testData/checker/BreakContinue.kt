class C {

    fun f (<warning>a</warning> : Boolean, <warning>b</warning> : Boolean) {
        @b (<error descr="[EXPRESSION_EXPECTED] While is not an expression, and only expressions are allowed here">while (true)
          @a {
            <error>break@f</error>
            break
            break@b
            <error>break@a</error>
          }</error>)

        <error>continue</error>

        @b (<error descr="[EXPRESSION_EXPECTED] While is not an expression, and only expressions are allowed here">while (true)
          @a {
            <error>continue@f</error>
            continue
            continue@b
            <error>continue@a</error>
          }</error>)

        <error>break</error>

        <error>continue@f</error>
        <error>break@f</error>
    }

}