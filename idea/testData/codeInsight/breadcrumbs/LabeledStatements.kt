fun foo() {
    OuterLoop@
    for (i in 1..10) {
        for (j in 1..10) {
            Label1@ Label2@
            while(true) {
                DoWhile@
                do {
                    <caret>
                } while (x)
            }
        }
    }
}