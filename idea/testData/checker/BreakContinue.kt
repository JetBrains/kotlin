class C {

    fun f (<warning>a</warning> : Boolean, <warning>b</warning> : Boolean) {
        @b (while (true)
          @a {
            <error>break@f</error>
            break
            <warning>break@b</warning>
            <error>break@a</error>
          })

        <error>continue</error>

        @b (while (true)
          @a {
            <error>continue@f</error>
            continue
            <warning>continue@b</warning>
            <error>continue@a</error>
          })

        <error>break</error>

        <error>continue@f</error>
        <error>break@f</error>
    }

}