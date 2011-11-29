class C {

    fun f (<warning>a</warning> : Boolean, <warning>b</warning> : Boolean) {
        @b (while (true)
          @a {
            <error>break@f</error>
            break
            break@b
            <error>break@a</error>
          })

        <error>continue</error>

        @b (while (true)
          @a {
            <error>continue@f</error>
            continue
            continue@b
            <error>continue@a</error>
          })

        <error>break</error>

        <error>continue@f</error>
        <error>break@f</error>
    }

}