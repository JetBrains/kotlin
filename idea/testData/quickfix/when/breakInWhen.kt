// "Add label to loop" "true"
// LANGUAGE_VERSION: 1.3

fun breakContinueInWhen(i: Int) {
    for (y in 0..10) {
        when(i) {
            0 -> bre<caret>ak
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
/* IGNORE_FIR */
