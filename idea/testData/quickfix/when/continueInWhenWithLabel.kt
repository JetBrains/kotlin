// "Add label to loop" "true"
// LANGUAGE_VERSION: 1.3

fun breakContinueInWhen(i: Int) {
    loop@ for (x in 0..10) {
        for (y in 0..10) {
            when(i) {
                0 -> co<caret>ntinue
                2 -> {
                for(z in 0..10) {
                    break
                }
                for(w in 0..10) {
                    continue
                }
            }
            }
        }
    }
}
/* IGNORE_FIR */
