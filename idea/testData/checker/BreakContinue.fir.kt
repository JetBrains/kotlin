class C {

    fun f (a : Boolean, b : Boolean) {
        b@ (while (true)
          a@ {
            <error descr="[NOT_A_LOOP_LABEL] The label does not denote a loop">break@f</error>
            break
            break@b
            <error descr="[NOT_A_LOOP_LABEL] The label does not denote a loop">break@a</error>
          })

        <error descr="[BREAK_OR_CONTINUE_OUTSIDE_A_LOOP] 'break' and 'continue' are only allowed inside a loop">continue</error>

        b@ (while (true)
          a@ {
            <error descr="[NOT_A_LOOP_LABEL] The label does not denote a loop">continue@f</error>
            continue
            continue@b
            <error descr="[NOT_A_LOOP_LABEL] The label does not denote a loop">continue@a</error>
          })

        <error descr="[BREAK_OR_CONTINUE_OUTSIDE_A_LOOP] 'break' and 'continue' are only allowed inside a loop">break</error>

        <error descr="[BREAK_OR_CONTINUE_OUTSIDE_A_LOOP] 'break' and 'continue' are only allowed inside a loop">continue@f</error>
        <error descr="[BREAK_OR_CONTINUE_OUTSIDE_A_LOOP] 'break' and 'continue' are only allowed inside a loop">break@f</error>
    }

}
